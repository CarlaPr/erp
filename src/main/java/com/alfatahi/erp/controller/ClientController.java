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

    // Método unificado para listar e pesquisar
    @GetMapping
    public String listClients(@RequestParam(value = "search", required = false) String search, Model model) {
        model.addAttribute("currentPage", "clients");

        List<Client> clients;
        // Verifica se há um termo de pesquisa
        if (search != null && !search.trim().isEmpty()) {
            clients = clientService.getAllClients(search); // Método de busca que você criou no service
        } else {
            clients = clientService.listAllActive(); // Comportamento padrão: lista todos os ativos
        }

        model.addAttribute("clients", clients);
        model.addAttribute("search", search); // Mantém o termo na tela
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