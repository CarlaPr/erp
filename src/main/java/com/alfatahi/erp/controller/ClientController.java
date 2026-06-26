package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.Client;
import com.alfatahi.erp.service.ClientService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    public String listClients(Model model) {
        model.addAttribute("currentPage", "clients");
        model.addAttribute("clients", clientService.listAllActive());
        model.addAttribute("newClient", new Client()); // Objeto vazio para o modal de cadastro
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
        model.addAttribute("newClient", clientService.findById(id)); // Carrega os dados para o formulário
        model.addAttribute("isEditing", true);
        return "clients";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable UUID id) {
        clientService.delete(id);
        return "redirect:/clients";
    }
}