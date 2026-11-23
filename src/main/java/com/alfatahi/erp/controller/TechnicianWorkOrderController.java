package com.alfatahi.erp.controller;

import com.alfatahi.erp.security.PageAccessService;
import com.alfatahi.erp.security.PageModule;
import com.alfatahi.erp.service.TechnicianWorkOrderService;
import com.alfatahi.erp.service.TechnicianWorkOrderService.Filter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

@Controller
@RequestMapping("/technician-work-orders")
public class TechnicianWorkOrderController {
    private static final String PAGE = PageModule.TECHNICIAN_WORK_ORDERS.getKey();
    private final TechnicianWorkOrderService workOrders;
    private final PageAccessService access;

    public TechnicianWorkOrderController(TechnicianWorkOrderService workOrders, PageAccessService access) {
        this.workOrders = workOrders;
        this.access = access;
    }

    @ModelAttribute("currentPage")
    public String currentPage() { return PAGE; }

    @GetMapping
    public String list(@RequestParam(defaultValue = "active") String filter, Model model) {
        Filter selected;
        try {
            selected = Filter.valueOf(filter.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException invalidFilter) {
            selected = Filter.ACTIVE;
        }
        model.addAttribute("orders", workOrders.list(selected));
        model.addAttribute("filters", Filter.values());
        model.addAttribute("selectedFilter", selected);
        return "technician-work-orders";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        var permissions = access.access(PAGE);
        var detail = workOrders.detail(id, permissions.canEdit());
        model.addAttribute("detail", detail);
        model.addAttribute("canEdit", permissions.canEdit() && !detail.order().completed());
        model.addAttribute("canDelete", permissions.canDelete() && !detail.order().completed());
        return "technician-work-order";
    }

    @PostMapping("/{id}/items")
    public String addItem(@PathVariable UUID id, @Valid @ModelAttribute ItemForm form, BindingResult binding,
                          RedirectAttributes redirect) {
        if (binding.hasErrors()) return invalidItem(id, redirect);
        workOrders.addItem(id, form.name(), form.quantity(), form.observation());
        return checklistResult(id, true, "Item adicionado.", redirect);
    }

    @PostMapping("/{id}/items/{itemId}/edit")
    public String editItem(@PathVariable UUID id, @PathVariable UUID itemId, @RequestParam long version,
                           @Valid @ModelAttribute ItemForm form, BindingResult binding, RedirectAttributes redirect) {
        if (binding.hasErrors()) return invalidItem(id, redirect);
        boolean saved = workOrders.editItem(id, itemId, version, form.name(), form.quantity(), form.observation());
        return checklistResult(id, saved, "Item atualizado.", redirect);
    }

    @PostMapping("/{id}/items/{itemId}/completion")
    public String completeItem(@PathVariable UUID id, @PathVariable UUID itemId, @RequestParam long version,
                               @RequestParam boolean completed, RedirectAttributes redirect) {
        boolean saved = workOrders.markItem(id, itemId, version, completed);
        return checklistResult(id, saved, completed ? "Item concluído." : "Item marcado como pendente.", redirect);
    }

    @PostMapping("/{id}/items/{itemId}/delete")
    public String deleteItem(@PathVariable UUID id, @PathVariable UUID itemId, @RequestParam long version,
                             RedirectAttributes redirect) {
        return checklistResult(id, workOrders.deleteItem(id, itemId, version), "Item excluído.", redirect);
    }

    @PostMapping("/{id}/suggestions")
    public String suggestions(@PathVariable UUID id, RedirectAttributes redirect) {
        int added = workOrders.generateSuggestions(id);
        String message = switch (added) {
            case 0 -> "Nenhuma sugestão nova para os serviços cadastrados.";
            case 1 -> "1 sugestão adicionada.";
            default -> added + " sugestões adicionadas.";
        };
        return checklistResult(id, true, message, redirect);
    }

    @PostMapping("/{id}/notes")
    public String addNote(@PathVariable UUID id, @Valid @ModelAttribute NoteForm form, BindingResult binding,
                          Authentication authentication, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            redirect.addFlashAttribute("noteError", "Escreva uma observação de até 20 mil caracteres.");
        } else {
            workOrders.addNote(id, authentication.getName(), form.body());
            redirect.addFlashAttribute("noteMessage", "Observação registrada.");
        }
        return "redirect:/technician-work-orders/" + id + "#observacoes";
    }

    private String invalidItem(UUID id, RedirectAttributes redirect) {
        redirect.addFlashAttribute("checklistError",
                "Informe um nome de até 180 caracteres, uma quantidade positiva (opcional) e até 2 mil caracteres de observação.");
        return "redirect:/technician-work-orders/" + id + "#checklist";
    }

    private String checklistResult(UUID id, boolean success, String message, RedirectAttributes redirect) {
        if (success) redirect.addFlashAttribute("checklistMessage", message);
        else redirect.addFlashAttribute("checklistError", "Este item mudou desde que você abriu a página. Confira os dados atuais e tente novamente.");
        return "redirect:/technician-work-orders/" + id + "#checklist";
    }

    public record ItemForm(@NotBlank @Size(max = 180) String name,
                           @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal quantity,
                           @Size(max = 2000) String observation) {}

    public record NoteForm(@NotBlank @Size(max = 20000) String body) {}
}
