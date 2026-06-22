package com.alfatahi.erp.service;

import com.alfatahi.erp.entity.Client;
import com.alfatahi.erp.repository.ClientRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public List<Client> listAllActive() {
        return clientRepository.findByIsActiveTrueOrderByNameAsc();
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

    public void delete(UUID id) {
        // Exclusão lógica ou física. Conforme o script SQL original, faremos a deleção física
        clientRepository.deleteById(id);
    }
}