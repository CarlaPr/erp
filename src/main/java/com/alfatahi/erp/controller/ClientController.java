package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.Client;
import com.alfatahi.erp.service.ClientService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    public String listClients(@RequestParam(value = "search", required = false) String search,
                              @RequestParam(value = "page", defaultValue = "0") int page,
                              Model model) {
        addClientsPage(search, page, model);
        model.addAttribute("newClient", new Client());

        return "clients";
    }

    @GetMapping("/results")
    public String searchClients(@RequestParam(value = "search", required = false) String search,
                                @RequestParam(value = "page", defaultValue = "0") int page,
                                Model model) {
        addClientsPage(search, page, model);
        return "clients :: clientResults";
    }

    @PostMapping("/save")
    public String saveClient(@ModelAttribute("newClient") Client client,
                             @RequestParam(value = "search", required = false) String search,
                             @RequestParam(value = "page", defaultValue = "0") int page,
                             RedirectAttributes redirectAttributes) {
        clientService.save(client);
        addListRedirectAttributes(search, page, redirectAttributes);
        return "redirect:/clients";
    }

    @GetMapping("/edit/{id}")
    public String editClientForm(@PathVariable("id") UUID id,
                                 @RequestParam(value = "search", required = false) String search,
                                 @RequestParam(value = "page", defaultValue = "0") int page,
                                 Model model) {
        addClientsPage(search, page, model);
        model.addAttribute("newClient", clientService.findById(id));
        model.addAttribute("isEditing", true);
        return "clients";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable("id") UUID id,
                         @RequestParam(value = "search", required = false) String search,
                         @RequestParam(value = "page", defaultValue = "0") int page,
                         RedirectAttributes redirectAttributes) {
        clientService.delete(id);
        addListRedirectAttributes(search, page, redirectAttributes);
        return "redirect:/clients";
    }

    private void addClientsPage(String search, int page, Model model) {
        String normalizedSearch = search == null ? "" : search.trim();
        Page<Client> clients = clientService.listPage(normalizedSearch, page);
        model.addAttribute("currentPage", "clients");
        model.addAttribute("clients", clients.getContent());
        model.addAttribute("clientPage", clients);
        model.addAttribute("search", normalizedSearch);
    }

    private void addListRedirectAttributes(String search, int page, RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute("page", Math.max(page, 0));
        if (search != null && !search.trim().isEmpty()) {
            redirectAttributes.addAttribute("search", search.trim());
        }
    }
}
