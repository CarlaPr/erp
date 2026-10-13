package com.alfatahi.erp.controller;

import com.alfatahi.erp.dto.DreEntry;
import com.alfatahi.erp.dto.DreReport;
import com.alfatahi.erp.service.DreGroup;
import com.alfatahi.erp.service.DreService;
import com.alfatahi.erp.service.FinancialPeriod;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/dre")
public class DreController {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final DreService dreService;

    public DreController(DreService dreService) {
        this.dreService = dreService;
    }

    @GetMapping
    public String index(@RequestParam(required = false, defaultValue = "month") String view,
                        @RequestParam(required = false) String month,
                        @RequestParam(required = false) Integer ano,
                        Model model) {
        Selection selection = select(view, month, ano);
        DreReport report = selection.yearly() ? dreService.year(selection.year()) : dreService.month(selection.reference());

        YearMonth rangeFrom = selection.yearly() ? YearMonth.of(selection.year(), 1) : selection.reference();
        YearMonth rangeTo = selection.yearly() ? YearMonth.of(selection.year(), 12) : selection.reference();
        int currentYear = FinancialPeriod.referenceFor(FinancialPeriod.today()).getYear();
        List<Integer> years = new ArrayList<>();
        for (int y = currentYear + 1; y >= currentYear - 5; y--) years.add(y);

        model.addAttribute("currentPage", "dre");
        model.addAttribute("view", selection.yearly() ? "year" : "month");
        model.addAttribute("report", report);
        model.addAttribute("monthValue", selection.reference().toString());
        model.addAttribute("prevMonthValue", selection.reference().minusMonths(1).toString());
        model.addAttribute("nextMonthValue", selection.reference().plusMonths(1).toString());
        model.addAttribute("ano", selection.year());
        model.addAttribute("years", years);
        model.addAttribute("periodDescription", dreService.periodDescription(selection.reference()));
        model.addAttribute("rangeFrom", rangeFrom.toString());
        model.addAttribute("rangeTo", rangeTo.toString());
        model.addAttribute("monthKeys", report.months().stream().map(YearMonth::toString).toList());
        return "dre";
    }

    @GetMapping("/export")
    public void export(@RequestParam(required = false, defaultValue = "month") String view,
                       @RequestParam(required = false) String month,
                       @RequestParam(required = false) Integer ano,
                       HttpServletResponse response) throws Exception {
        Selection selection = select(view, month, ano);
        DreReport report = selection.yearly() ? dreService.year(selection.year()) : dreService.month(selection.reference());

        String fileName = selection.yearly() ? "dre_" + selection.year() + ".csv"
                : "dre_" + selection.reference() + ".csv";
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.getOutputStream().write(0xEF);
        response.getOutputStream().write(0xBB);
        response.getOutputStream().write(0xBF);

        DecimalFormat money = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(PT_BR));
        PrintWriter writer = new PrintWriter(new java.io.OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8));
        writer.println("DRE gerencial - " + report.focusLabel() + " (competencia do dia 6 ao dia 5)");
        StringBuilder header = new StringBuilder("Descricao");
        for (String label : report.monthLabels()) header.append(';').append(label);
        if (report.showTotal()) header.append(";Total;AV % (total)");
        else header.append(";AV % (ultimo mes);Variacao %");
        writer.println(header);

        for (DreReport.Line line : report.lines()) {
            StringBuilder row = new StringBuilder(clean(line.label()));
            if (line.kind() == DreReport.Kind.SECTION) {
                writer.println(row);
                continue;
            }
            for (BigDecimal value : line.values()) row.append(';').append(money.format(value));
            if (report.showTotal()) {
                row.append(';').append(money.format(line.total()));
                row.append(';').append(money.format(line.avTotal()));
            } else {
                row.append(';').append(money.format(line.av()[line.av().length - 1]));
                row.append(';').append(line.variation() == null ? "" : money.format(line.variation()));
            }
            writer.println(row);
            for (DreReport.Detail detail : line.details()) {
                StringBuilder sub = new StringBuilder("    " + clean(detail.label()));
                for (BigDecimal value : detail.values()) sub.append(';').append(money.format(value));
                if (report.showTotal()) sub.append(';').append(money.format(detail.total())).append(';');
                else sub.append(";;");
                writer.println(sub);
            }
        }
        writer.flush();
    }

    @GetMapping("/lancamentos")
    @ResponseBody
    public Map<String, Object> entries(@RequestParam String group,
                                       @RequestParam String from,
                                       @RequestParam String to,
                                       @RequestParam(required = false) String category,
                                       @RequestParam(required = false) String subcategory) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<DreEntry> entries;
        try {
            YearMonth start = YearMonth.parse(from);
            YearMonth end = YearMonth.parse(to);
            if (end.isBefore(start)) {
                YearMonth swap = start;
                start = end;
                end = swap;
            }
            entries = dreService.entries(group, start, end, category, subcategory);
        } catch (DateTimeParseException e) {
            entries = List.of();
        }
        BigDecimal total = entries.stream().map(DreEntry::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        result.put("title", titleFor(group));
        result.put("count", entries.size());
        result.put("total", total);
        result.put("entries", entries);
        return result;
    }


    @GetMapping("/mapeamento")
    public String mapping(Model model) {
        Map<DreGroup.Section, List<DreGroup>> groupsBySection = new EnumMap<>(DreGroup.Section.class);
        for (DreGroup group : DreGroup.selectable()) {
            groupsBySection.computeIfAbsent(group.getSection(), s -> new ArrayList<>()).add(group);
        }
        model.addAttribute("currentPage", "dre");
        model.addAttribute("tree", dreService.mappingTree());
        model.addAttribute("groupsBySection", groupsBySection);
        return "dre-mapeamento";
    }

    @PostMapping("/mapeamento/salvar")
    public String saveMapping(@RequestParam(required = false) List<UUID> categoryId,
                              @RequestParam(required = false) List<String> categoryGroup,
                              @RequestParam(required = false) List<UUID> subId,
                              @RequestParam(required = false) List<String> subGroup,
                              @RequestParam(required = false) List<String> subBehavior,
                              RedirectAttributes redirect) {
        try {
            dreService.saveMapping(categoryId, categoryGroup, subId, subGroup, subBehavior);
            redirect.addFlashAttribute("success", "Plano de contas da DRE salvo. A DRE já reflete a nova classificação.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dre/mapeamento";
    }


    private record Selection(boolean yearly, YearMonth reference, int year) { }

    private Selection select(String view, String month, Integer ano) {
        YearMonth current = FinancialPeriod.referenceFor(FinancialPeriod.today());
        YearMonth reference = current;
        if (month != null && !month.isBlank()) {
            try {
                reference = YearMonth.parse(month);
            } catch (DateTimeParseException ignored) {
                reference = current;
            }
        }
        int year = ano != null && ano >= 2000 && ano <= 2100 ? ano : reference.getYear();
        return new Selection("year".equalsIgnoreCase(view), reference, year);
    }

    private static String titleFor(String group) {
        return switch (group == null ? "" : group) {
            case DreService.KEY_REVENUE -> "Receita operacional bruta";
            case DreService.KEY_DISCOUNTS -> "Descontos concedidos";
            case DreService.KEY_LOSSES -> "Perdas registradas";
            default -> DreGroup.isValidKey(group) ? DreGroup.fromKey(group).getLabel() : "Lançamentos";
        };
    }

    private static String clean(String text) {
        return text == null ? "" : text.replace(";", ",").replace("\n", " ");
    }
}
