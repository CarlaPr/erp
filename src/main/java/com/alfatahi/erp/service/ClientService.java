package com.alfatahi.erp.service;

import com.alfatahi.erp.entity.Client;
import com.alfatahi.erp.repository.ClientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class ClientService {

    private static final int CLIENTS_PAGE_SIZE = 40;

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public List<Client> listAllActive() {
        return clientRepository.findByIsActiveTrueOrderByNameAsc();
    }

    public Page<Client> listPage(String search, int page) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), CLIENTS_PAGE_SIZE, Sort.by("name", "id"));
        String normalizedSearch = search == null ? "" : search.trim();
        Page<Client> clients = findPage(normalizedSearch, pageable);

        if (pageable.getPageNumber() > 0 && clients.isEmpty()) {
            int lastPage = Math.max(clients.getTotalPages() - 1, 0);
            return findPage(normalizedSearch, pageable.withPage(lastPage));
        }
        return clients;
    }

    private Page<Client> findPage(String search, Pageable pageable) {
        return search.isEmpty()
                ? clientRepository.findByIsActiveTrue(pageable)
                : clientRepository.searchClients(search, pageable);
    }

    public Client save(Client client) {
        if (client.getType() == null || client.getType().isEmpty()) {
            client.setType("individual");
        }
        return clientRepository.save(client);
    }

    public Client findById(UUID id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado com o ID: " + id));
    }

    public List<Client> getAllClients(String search) {
        if (search != null && !search.trim().isEmpty()) {
            return clientRepository.searchClients(search);
        }
        return clientRepository.findByIsActiveTrueOrderByNameAsc();
    }

    public void delete(UUID id) {
        Client client = findById(id);
        client.setIsActive(false);
        clientRepository.save(client);
    }
}
