package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.Client;
import com.alfatahi.erp.service.ClientService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    public String listClients(@RequestParam(value = "search", required = false) String search, Model model) {
        model.addAttribute("currentPage", "clients");

        List<Client> clients;

        if (search != null && !search.trim().isEmpty()) {
            clients = clientService.getAllClients(search);
        } else {
            clients = clientService.listAllActive();
        }

        model.addAttribute("clients", clients);
        model.addAttribute("search", search);
        model.addAttribute("newClient", new Client());

        return "clients";
    }

    @PostMapping("/save")
    public String saveClient(@ModelAttribute("newClient") Client client) {
        clientService.save(client);
        return "redirect:/clients";
    }

    @GetMapping("/edit/{id}")
    public String editClientForm(@PathVariable("id") UUID id, Model model) {
        model.addAttribute("currentPage", "clients");
        model.addAttribute("clients", clientService.listAllActive());
        model.addAttribute("newClient", clientService.findById(id));
        model.addAttribute("isEditing", true);
        return "clients";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable UUID id) {
        clientService.delete(id);
        return "redirect:/clients";
    }
}