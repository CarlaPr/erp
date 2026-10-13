package com.alfatahi.erp.service;

import com.alfatahi.erp.dto.DreEntry;
import com.alfatahi.erp.dto.DreReport;
import com.alfatahi.erp.dto.DreReport.Detail;
import com.alfatahi.erp.dto.DreReport.Kind;
import com.alfatahi.erp.dto.DreReport.Kpis;
import com.alfatahi.erp.dto.DreReport.Line;
import com.alfatahi.erp.entity.ExpenseCategory;
import com.alfatahi.erp.entity.ExpenseSubcategory;
import com.alfatahi.erp.repository.DreRepository;
import com.alfatahi.erp.repository.DreRepository.PayableRow;
import com.alfatahi.erp.repository.DreRepository.ReceivableRow;
import com.alfatahi.erp.repository.ExpenseCategoryRepository;
import com.alfatahi.erp.repository.ExpenseSubcategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * DRE gerencial por competência.
 *
 * Regras:
 *  - Receita: O.S. não cancelada de orçamento aprovado (mês da aprovação, igual ao Dashboard);
 *  - Despesas e custos: contas a pagar não canceladas, pelo mês de competência, valor total
 *    (pagas ou não), classificadas pela linha da DRE da subcategoria (plano de contas);
 *  - Descontos concedidos e taxas de cartão: registrados no recebimento (mês do pagamento);
 *  - Investimentos, principal de dívidas, reservas e retiradas ficam fora do resultado.
 */
@Service
@Transactional(readOnly = true)
public class DreService {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter SHORT_MONTH = DateTimeFormatter.ofPattern("MMM/yy", PT_BR);
    private static final DateTimeFormatter LONG_MONTH = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", PT_BR);
    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public static final String KEY_REVENUE = "RECEITA_BRUTA";
    public static final String KEY_DISCOUNTS = "DED_DESCONTOS";
    public static final String KEY_LOSSES = "PERDAS";

    private static final List<DreGroup> COST_GROUPS = List.of(
            DreGroup.CMV_MATERIAIS, DreGroup.CMV_SERV_TERCEIROS, DreGroup.CMV_INSTALACAO);
    private static final List<DreGroup> OPERATING_GROUPS = List.of(
            DreGroup.DESP_PESSOAL, DreGroup.DESP_COMERCIAL, DreGroup.DESP_OCUPACAO, DreGroup.DESP_ADMIN,
            DreGroup.DESP_MANUTENCAO, DreGroup.DESP_TRIBUTARIAS, DreGroup.DESP_OUTRAS, DreGroup.NAO_CLASSIFICADO);
    private static final List<DreGroup> FINANCIAL_GROUPS = List.of(
            DreGroup.DESP_FIN_CARTAO, DreGroup.DESP_FINANCEIRAS);
    private static final List<DreGroup> OUTSIDE_GROUPS = List.of(
            DreGroup.FORA_INVESTIMENTOS, DreGroup.FORA_AMORTIZACAO, DreGroup.FORA_RESERVAS, DreGroup.FORA_RETIRADAS);

    private final DreRepository repository;
    private final ExpenseCategoryRepository categoryRepository;
    private final ExpenseSubcategoryRepository subcategoryRepository;

    public DreService(DreRepository repository,
                      ExpenseCategoryRepository categoryRepository,
                      ExpenseSubcategoryRepository subcategoryRepository) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
        this.subcategoryRepository = subcategoryRepository;
    }


    public DreReport month(YearMonth reference) {
        return build("month", List.of(reference.minusMonths(1), reference), false, true,
                capitalize(reference.format(LONG_MONTH)));
    }

    public DreReport year(int year) {
        List<YearMonth> months = new ArrayList<>();
        for (int m = 1; m <= 12; m++) months.add(YearMonth.of(year, m));
        return build("year", months, true, false, "Ano " + year);
    }

    public String periodDescription(YearMonth reference) {
        FinancialPeriod period = FinancialPeriod.monthly(reference);
        return period.from().format(BR_DATE) + " a " + period.to().format(BR_DATE);
    }

    public List<DreEntry> entries(String key, YearMonth from, YearMonth to, String category, String subcategory) {
        LocalDate start = FinancialPeriod.monthly(from).from();
        LocalDate endExclusive = FinancialPeriod.monthly(to).endExclusive();
        List<DreEntry> result = new ArrayList<>();

        if (KEY_REVENUE.equals(key)) {
            repository.revenue(start, endExclusive).forEach(r -> result.add(new DreEntry(
                    r.date().format(BR_DATE), nz(r.number()) + " — " + nz(r.title()), nz(r.client()),
                    "Venda aprovada", "", nz(r.amount()))));
            return result;
        }
        if (KEY_DISCOUNTS.equals(key)) {
            for (ReceivableRow r : repository.receivableAdjustments(start, endExclusive)) {
                if (nz(r.discount()).signum() > 0) {
                    result.add(new DreEntry(r.paymentDate().format(BR_DATE), nz(r.description()), nz(r.client()),
                            "Desconto concedido" + orderSuffix(r.workOrder()), "Recebido", nz(r.discount())));
                }
            }
            return result;
        }
        if (KEY_LOSSES.equals(key)) {
            repository.losses(start, endExclusive).forEach(l -> result.add(new DreEntry(
                    l.date().format(BR_DATE), nz(l.description()), "", nz(l.type()) + orderSuffix(l.workOrder()),
                    "Registrada", nz(l.amount()))));
            return result;
        }

        if (!DreGroup.isValidKey(key)) return result;
        DreGroup group = DreGroup.fromKey(key);
        Catalog catalog = loadCatalog();
        for (PayableRow p : repository.payables(from, to)) {
            Resolved resolved = catalog.resolve(p);
            if (resolved.group() != group) continue;
            if (category != null && !category.isBlank() && !category.equalsIgnoreCase(resolved.category())) continue;
            if (subcategory != null && !subcategory.isBlank() && !subcategory.equalsIgnoreCase(resolved.subcategory())) continue;
            result.add(new DreEntry(p.dueDate().format(BR_DATE), nz(p.description()),
                    p.supplier() == null ? "Avulso" : p.supplier(),
                    resolved.label() + orderSuffix(p.workOrder()), statusLabel(p.status()), nz(p.total())));
        }
        if (group == DreGroup.DESP_FIN_CARTAO && (category == null || category.isBlank() || "AR".equalsIgnoreCase(category))) {
            for (ReceivableRow r : repository.receivableAdjustments(start, endExclusive)) {
                if (nz(r.fee()).signum() > 0) {
                    result.add(new DreEntry(r.paymentDate().format(BR_DATE), nz(r.description()), nz(r.client()),
                            "Taxa de cartão no recebimento" + orderSuffix(r.workOrder()), "Recebido", nz(r.fee())));
                }
            }
        }
        return result;
    }


    public record SubMapping(UUID id, String code, String name, String groupLabel, boolean active,
                             String dreGroup, String costBehavior) { }

    public record CategoryMapping(UUID id, String code, String name, boolean active, String dreGroup,
                                  List<SubMapping> subs) { }

    public List<CategoryMapping> mappingTree() {
        Map<UUID, List<SubMapping>> subsByCategory = new LinkedHashMap<>();
        for (ExpenseSubcategory s : subcategoryRepository.findAllByOrderByDisplayOrderAsc()) {
            if (s.getCategory() == null) continue;
            subsByCategory.computeIfAbsent(s.getCategory().getId(), k -> new ArrayList<>())
                    .add(new SubMapping(s.getId(), s.getCode(), s.getName(), s.getGroupLabel(), s.getActive(),
                            s.getDreGroup(), s.getCostBehavior()));
        }
        List<CategoryMapping> tree = new ArrayList<>();
        for (ExpenseCategory c : categoryRepository.findAllByOrderByDisplayOrderAsc()) {
            List<SubMapping> subs = subsByCategory.getOrDefault(c.getId(), List.of());
            if (!c.getActive() && subs.isEmpty()) continue;
            tree.add(new CategoryMapping(c.getId(), c.getCode(), c.getName(), c.getActive(), c.getDreGroup(), subs));
        }
        return tree;
    }

    @Transactional
    public void saveMapping(List<UUID> categoryIds, List<String> categoryGroups,
                            List<UUID> subcategoryIds, List<String> subcategoryGroups,
                            List<String> subcategoryBehaviors) {
        List<UUID> catIds = categoryIds == null ? List.of() : categoryIds;
        List<String> catGroups = categoryGroups == null ? List.of() : categoryGroups;
        List<UUID> subIds = subcategoryIds == null ? List.of() : subcategoryIds;
        List<String> subGroups = subcategoryGroups == null ? List.of() : subcategoryGroups;
        List<String> subBehaviors = subcategoryBehaviors == null ? List.of() : subcategoryBehaviors;
        if (catIds.size() != catGroups.size() || subIds.size() != subGroups.size() || subIds.size() != subBehaviors.size()) {
            throw new IllegalArgumentException("Dados do plano de contas inconsistentes. Recarregue a página e tente novamente.");
        }

        Map<UUID, ExpenseCategory> categories = categoryRepository.findAllById(catIds).stream()
                .collect(Collectors.toMap(ExpenseCategory::getId, Function.identity()));
        for (int i = 0; i < catIds.size(); i++) {
            ExpenseCategory category = categories.get(catIds.get(i));
            if (category == null) continue;
            category.setDreGroup(validGroup(catGroups.get(i)));
        }

        Map<UUID, ExpenseSubcategory> subcategories = subcategoryRepository.findAllById(subIds).stream()
                .collect(Collectors.toMap(ExpenseSubcategory::getId, Function.identity()));
        for (int i = 0; i < subIds.size(); i++) {
            ExpenseSubcategory subcategory = subcategories.get(subIds.get(i));
            if (subcategory == null) continue;
            String group = validGroup(subGroups.get(i));
            subcategory.setDreGroup(group);
            subcategory.setCostBehavior(group.startsWith("FORA_") ? null : validBehavior(subBehaviors.get(i)));
        }

        categoryRepository.saveAll(categories.values());
        subcategoryRepository.saveAll(subcategories.values());
        categoryRepository.flush();
        repository.refreshExpenseTypes();
    }

    private static String validGroup(String raw) {
        if (!DreGroup.isValidKey(raw)) {
            throw new IllegalArgumentException("Linha da DRE inválida: " + raw);
        }
        return DreGroup.fromKey(raw).name();
    }

    private static String validBehavior(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.strip().toUpperCase(Locale.ROOT);
        if (!value.equals("FIXA") && !value.equals("VARIAVEL")) {
            throw new IllegalArgumentException("Comportamento de custo inválido: " + raw);
        }
        return value;
    }


    private DreReport build(String view, List<YearMonth> months, boolean showTotal, boolean showVariation,
                            String focusLabel) {
        int n = months.size();
        YearMonth first = months.get(0);
        YearMonth last = months.get(n - 1);
        LocalDate start = FinancialPeriod.monthly(first).from();
        LocalDate endExclusive = FinancialPeriod.monthly(last).endExclusive();
        Catalog catalog = loadCatalog();

        BigDecimal[] revenue = zeros(n);
        BigDecimal[] discounts = zeros(n);
        BigDecimal[] losses = zeros(n);
        BigDecimal[] fixed = zeros(n);
        BigDecimal[] variable = zeros(n);
        Map<DreGroup, GroupAgg> groups = new EnumMap<>(DreGroup.class);
        long unclassifiedCount = 0;

        for (var r : repository.revenue(start, endExclusive)) {
            add(revenue, months, r.ref(), r.amount());
        }
        for (var l : repository.losses(start, endExclusive)) {
            add(losses, months, l.ref(), l.amount());
        }
        for (ReceivableRow r : repository.receivableAdjustments(start, endExclusive)) {
            int idx = months.indexOf(r.ref());
            if (idx < 0) continue;
            discounts[idx] = discounts[idx].add(nz(r.discount()));
            variable[idx] = variable[idx].add(nz(r.discount()));
            if (nz(r.fee()).signum() > 0) {
                groups.computeIfAbsent(DreGroup.DESP_FIN_CARTAO, g -> new GroupAgg(n))
                        .add(idx, r.fee(), "AR", "cartao", "Taxas de cartão › Recebimentos (maquininha)");
                variable[idx] = variable[idx].add(nz(r.fee()));
            }
        }
        for (PayableRow p : repository.payables(first, last)) {
            int idx = months.indexOf(p.ref());
            if (idx < 0) continue;
            Resolved resolved = catalog.resolve(p);
            BigDecimal amount = nz(p.total());
            groups.computeIfAbsent(resolved.group(), g -> new GroupAgg(n))
                    .add(idx, amount, resolved.category(), resolved.subcategory(), resolved.label());
            if (resolved.group() == DreGroup.NAO_CLASSIFICADO) unclassifiedCount++;
            DreGroup.Section section = resolved.group().getSection();
            if (resolved.group() == DreGroup.DED_IMPOSTOS_VENDAS) {
                variable[idx] = variable[idx].add(amount);
            } else if (section == DreGroup.Section.CUSTOS || section == DreGroup.Section.OPERACIONAIS
                    || section == DreGroup.Section.FINANCEIRO || section == DreGroup.Section.NAO_CLASSIFICADO) {
                if ("VARIAVEL".equals(resolved.behavior())) variable[idx] = variable[idx].add(amount);
                else fixed[idx] = fixed[idx].add(amount);
            }
        }

        BigDecimal[] taxesOnSales = neg(groupValues(groups, DreGroup.DED_IMPOSTOS_VENDAS, n));
        BigDecimal[] discountsNeg = neg(discounts);
        BigDecimal[] netRevenue = sum(n, revenue, discountsNeg, taxesOnSales);

        BigDecimal[] costs = zeros(n);
        for (DreGroup g : COST_GROUPS) costs = sum(n, costs, neg(groupValues(groups, g, n)));
        BigDecimal[] grossProfit = sum(n, netRevenue, costs);

        BigDecimal[] operating = zeros(n);
        for (DreGroup g : OPERATING_GROUPS) operating = sum(n, operating, neg(groupValues(groups, g, n)));
        BigDecimal[] operatingResult = sum(n, grossProfit, operating);

        BigDecimal[] financial = zeros(n);
        for (DreGroup g : FINANCIAL_GROUPS) financial = sum(n, financial, neg(groupValues(groups, g, n)));
        BigDecimal[] beforeTax = sum(n, operatingResult, financial);

        BigDecimal[] incomeTax = neg(groupValues(groups, DreGroup.IR_CSLL, n));
        BigDecimal[] netProfit = sum(n, beforeTax, incomeTax);

        BigDecimal[] outside = zeros(n);
        for (DreGroup g : OUTSIDE_GROUPS) outside = sum(n, outside, neg(groupValues(groups, g, n)));
        BigDecimal[] afterAllocations = sum(n, netProfit, outside);

        LineFactory f = new LineFactory(n, revenue, showTotal, showVariation, months);
        List<Line> lines = new ArrayList<>();
        lines.add(f.line(KEY_REVENUE, "(+) Receita operacional bruta",
                "Vendas aprovadas (O.S. não canceladas), pelo mês de aprovação do orçamento.",
                Kind.ITEM, revenue, List.of(), true, false));
        lines.add(f.line(KEY_DISCOUNTS, "(−) Descontos concedidos",
                "Descontos dados no recebimento (mês do pagamento).",
                Kind.ITEM, discountsNeg, List.of(), true, false));
        lines.add(f.line(DreGroup.DED_IMPOSTOS_VENDAS.name(), "(−) " + DreGroup.DED_IMPOSTOS_VENDAS.getLabel(),
                null, Kind.ITEM, taxesOnSales, details(groups, DreGroup.DED_IMPOSTOS_VENDAS, n), true, false));
        lines.add(f.line("RECEITA_LIQUIDA", "(=) Receita operacional líquida", null, Kind.SUBTOTAL, netRevenue,
                List.of(), false, false));

        for (DreGroup g : COST_GROUPS) {
            lines.add(f.group(g, groups, n, "(−) ", false));
        }
        lines.add(f.line("LUCRO_BRUTO", "(=) Lucro bruto", "Receita líquida menos o custo dos serviços.",
                Kind.RESULT, grossProfit, List.of(), false, false));

        for (DreGroup g : OPERATING_GROUPS) {
            boolean hideWhenZero = g == DreGroup.DESP_OUTRAS || g == DreGroup.NAO_CLASSIFICADO;
            Line line = f.group(g, groups, n, "(−) ", g == DreGroup.NAO_CLASSIFICADO);
            if (hideWhenZero && isZero(line.values())) continue;
            lines.add(line);
        }
        lines.add(f.line("EBITDA", "(=) Resultado operacional (EBITDA)",
                "Lucro antes do resultado financeiro e do imposto sobre o lucro (sem depreciação lançada).",
                Kind.RESULT, operatingResult, List.of(), false, false));

        for (DreGroup g : FINANCIAL_GROUPS) {
            lines.add(f.group(g, groups, n, "(−) ", false));
        }
        lines.add(f.line("LAIR", "(=) Resultado antes do IRPJ/CSLL", null, Kind.SUBTOTAL, beforeTax,
                List.of(), false, false));
        lines.add(f.group(DreGroup.IR_CSLL, groups, n, "(−) ", false));
        lines.add(f.line("LUCRO_LIQUIDO", "(=) Lucro líquido do período", null, Kind.RESULT, netProfit,
                List.of(), false, false));

        lines.add(f.line("FORA_HEADER", "Saídas fora da DRE (não alteram o lucro)", null, Kind.SECTION,
                zeros(n), List.of(), false, false));
        for (DreGroup g : OUTSIDE_GROUPS) {
            lines.add(f.group(g, groups, n, "(−) ", false));
        }
        lines.add(f.line("SOBRA", "(=) Resultado após investimentos, dívidas, reservas e retiradas",
                "Lucro líquido menos as saídas acima: o que sobrou de verdade para o caixa da empresa.",
                Kind.SUBTOTAL, afterAllocations, List.of(), false, false));
        lines.add(f.line(KEY_LOSSES, "Memo: perdas registradas no Controle de Perdas",
                "Informativo. Já estão refletidas nos custos pagos (reposição de material); não é somado ao resultado.",
                Kind.MEMO, losses, List.of(), true, false));

        BigDecimal grossFocus = focus(revenue, showTotal);
        BigDecimal netRevenueFocus = focus(netRevenue, showTotal);
        BigDecimal grossProfitFocus = focus(grossProfit, showTotal);
        BigDecimal operatingFocus = focus(operatingResult, showTotal);
        BigDecimal netProfitFocus = focus(netProfit, showTotal);
        BigDecimal variableFocus = focus(variable, showTotal);
        BigDecimal fixedFocus = focus(fixed, showTotal);
        BigDecimal contribution = grossFocus.subtract(variableFocus);
        BigDecimal contributionPct = pct(contribution, grossFocus);
        BigDecimal breakEven = contributionPct.signum() > 0 && fixedFocus.signum() > 0
                ? fixedFocus.multiply(HUNDRED).divide(contributionPct, 2, RoundingMode.HALF_UP) : null;
        BigDecimal lossesFocus = focus(losses, showTotal);
        Kpis kpis = new Kpis(grossFocus, netRevenueFocus, grossProfitFocus, pct(grossProfitFocus, grossFocus),
                operatingFocus, pct(operatingFocus, grossFocus), netProfitFocus, pct(netProfitFocus, grossFocus),
                contribution, contributionPct, fixedFocus, breakEven, lossesFocus, pct(lossesFocus, grossFocus),
                focus(outside, showTotal).negate(), focus(afterAllocations, showTotal));

        GroupAgg unclassified = groups.get(DreGroup.NAO_CLASSIFICADO);
        BigDecimal unclassifiedTotal = unclassified == null ? BigDecimal.ZERO : unclassified.total();

        List<String> labels = months.stream()
                .map(m -> capitalize(m.format(SHORT_MONTH).replace(".", "")))
                .toList();
        return new DreReport(view, List.copyOf(months), labels, showTotal, showVariation, focusLabel, lines, kpis,
                unclassifiedTotal, unclassifiedCount);
    }


    private record CatInfo(String code, String name, String defaultGroup) { }

    private record SubInfo(String name, String group, String behavior) { }

    private record Resolved(DreGroup group, String category, String subcategory, String label, String behavior) { }

    private static final class Catalog {
        private final Map<String, CatInfo> categories = new LinkedHashMap<>();
        private final Map<String, SubInfo> subcategories = new LinkedHashMap<>();

        Resolved resolve(PayableRow p) {
            String cat = normalize(p.category());
            String sub = p.subcategory() == null ? "" : p.subcategory().strip().toLowerCase(Locale.ROOT);
            if (p.financialExpense()) {
                return new Resolved(DreGroup.DESP_FIN_CARTAO, cat, sub, "Taxas de cartão (lançamentos legados)", "VARIAVEL");
            }
            CatInfo c = categories.get(cat);
            SubInfo s = sub.isEmpty() ? null : subcategories.get(cat + "::" + sub);
            String catName = c != null ? c.name() : (cat.isEmpty() ? "(sem categoria)" : cat);
            String subName = s != null ? s.name() : (sub.isEmpty() ? "(sem subcategoria)" : sub + " (não cadastrada)");

            DreGroup group;
            if (s != null && s.group() != null && !s.group().isBlank()) {
                group = DreGroup.fromKey(s.group());
            } else if (c != null && c.defaultGroup() != null && !c.defaultGroup().isBlank()) {
                group = DreGroup.fromKey(c.defaultGroup());
            } else {
                group = DreGroup.NAO_CLASSIFICADO;
            }
            return new Resolved(group, cat, sub, catName + " › " + subName, behavior(s, p.expenseType()));
        }

        private static String behavior(SubInfo s, String expenseType) {
            if (s != null && s.behavior() != null) return s.behavior();
            return "VARIAVEL".equalsIgnoreCase(expenseType) ? "VARIAVEL" : "FIXA";
        }
    }

    private Catalog loadCatalog() {
        Catalog catalog = new Catalog();
        for (ExpenseCategory c : categoryRepository.findAllByOrderByDisplayOrderAsc()) {
            catalog.categories.put(normalize(c.getCode()), new CatInfo(c.getCode(), c.getName(), c.getDreGroup()));
        }
        for (ExpenseSubcategory s : subcategoryRepository.findAllByOrderByDisplayOrderAsc()) {
            if (s.getCategory() == null) continue;
            catalog.subcategories.putIfAbsent(
                    normalize(s.getCategory().getCode()) + "::" + s.getCode().strip().toLowerCase(Locale.ROOT),
                    new SubInfo(s.getName(), s.getDreGroup(), s.getCostBehavior()));
        }
        return catalog;
    }


    private static final class GroupAgg {
        private final int size;
        private final BigDecimal[] totals;
        private final Map<String, DetailAgg> details = new LinkedHashMap<>();

        GroupAgg(int size) {
            this.size = size;
            this.totals = zeros(size);
        }

        void add(int idx, BigDecimal amount, String category, String subcategory, String label) {
            BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
            totals[idx] = totals[idx].add(value);
            DetailAgg detail = details.computeIfAbsent(category + "::" + subcategory,
                    k -> new DetailAgg(category, subcategory, label, size));
            detail.values[idx] = detail.values[idx].add(value);
        }

        BigDecimal total() {
            return sumAll(totals);
        }
    }

    private static final class DetailAgg {
        private final String category;
        private final String subcategory;
        private final String label;
        private final BigDecimal[] values;

        DetailAgg(String category, String subcategory, String label, int size) {
            this.category = category;
            this.subcategory = subcategory;
            this.label = label;
            this.values = zeros(size);
        }
    }

    private static BigDecimal[] groupValues(Map<DreGroup, GroupAgg> groups, DreGroup group, int n) {
        GroupAgg agg = groups.get(group);
        return agg == null ? zeros(n) : agg.totals.clone();
    }

    private static List<Detail> details(Map<DreGroup, GroupAgg> groups, DreGroup group, int n) {
        GroupAgg agg = groups.get(group);
        if (agg == null) return List.of();
        return agg.details.values().stream()
                .sorted(Comparator.comparing((DetailAgg d) -> sumAll(d.values)).reversed())
                .map(d -> new Detail(d.category, d.subcategory, d.label, neg(d.values), sumAll(d.values).negate()))
                .toList();
    }

    private static final class LineFactory {
        private final int n;
        private final BigDecimal[] base;
        private final boolean showTotal;
        private final boolean showVariation;
        private final List<YearMonth> months;

        LineFactory(int n, BigDecimal[] base, boolean showTotal, boolean showVariation, List<YearMonth> months) {
            this.n = n;
            this.base = base;
            this.showTotal = showTotal;
            this.showVariation = showVariation;
            this.months = months;
        }

        Line group(DreGroup group, Map<DreGroup, GroupAgg> groups, int size, String prefix, boolean alert) {
            BigDecimal[] values = neg(groupValues(groups, group, size));
            String label = prefix + group.getLabel();
            String hint = alert ? "Contas a pagar sem linha da DRE definida. Ajuste em Plano de contas da DRE." : null;
            return line(group.name(), label, hint, Kind.ITEM, values, details(groups, group, size), true, alert);
        }

        Line line(String key, String label, String hint, Kind kind, BigDecimal[] values, List<Detail> details,
                  boolean drillable, boolean alert) {
            BigDecimal[] av = new BigDecimal[n];
            for (int i = 0; i < n; i++) av[i] = pct(values[i], base[i]);
            BigDecimal total = showTotal ? sumAll(values) : null;
            BigDecimal avTotal = showTotal ? pct(sumAll(values), sumAll(base)) : null;
            BigDecimal variation = null;
            if (showVariation && n >= 2 && kind != Kind.SECTION) {
                BigDecimal previous = values[n - 2];
                BigDecimal current = values[n - 1];
                if (previous.signum() != 0) {
                    variation = current.subtract(previous).multiply(HUNDRED)
                            .divide(previous.abs(), 1, RoundingMode.HALF_UP);
                }
            }
            return new Line(key, label, hint, kind, values, total, av, avTotal, variation, alert, drillable,
                    details == null ? List.of() : details);
        }
    }


    private static BigDecimal[] zeros(int n) {
        BigDecimal[] values = new BigDecimal[n];
        for (int i = 0; i < n; i++) values[i] = BigDecimal.ZERO;
        return values;
    }

    private static BigDecimal[] neg(BigDecimal[] values) {
        BigDecimal[] result = new BigDecimal[values.length];
        for (int i = 0; i < values.length; i++) result[i] = values[i].negate();
        return result;
    }

    private static BigDecimal[] sum(int n, BigDecimal[]... arrays) {
        BigDecimal[] result = zeros(n);
        for (BigDecimal[] array : arrays) {
            for (int i = 0; i < n; i++) result[i] = result[i].add(array[i]);
        }
        return result;
    }

    private static BigDecimal sumAll(BigDecimal[] values) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal value : values) total = total.add(value);
        return total;
    }

    private static boolean isZero(BigDecimal[] values) {
        for (BigDecimal value : values) if (value.signum() != 0) return false;
        return true;
    }

    private static BigDecimal focus(BigDecimal[] values, boolean total) {
        return total ? sumAll(values) : values[values.length - 1];
    }

    private static void add(BigDecimal[] target, List<YearMonth> months, YearMonth ref, BigDecimal value) {
        int idx = months.indexOf(ref);
        if (idx >= 0) target[idx] = target[idx].add(nz(value));
    }

    private static BigDecimal pct(BigDecimal value, BigDecimal base) {
        if (base == null || base.signum() == 0) return BigDecimal.ZERO;
        return value.multiply(HUNDRED).divide(base, 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }

    private static String normalize(String code) {
        return code == null ? "" : code.strip().toUpperCase(Locale.ROOT);
    }

    private static String orderSuffix(String workOrder) {
        return workOrder == null || workOrder.isBlank() ? "" : " · " + workOrder;
    }

    private static String statusLabel(String status) {
        if (status == null) return "";
        return switch (status) {
            case "paid" -> "Pago";
            case "partial" -> "Parcial";
            case "pending" -> "Pendente";
            default -> status;
        };
    }

    private static String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase(PT_BR) + text.substring(1);
    }
}
