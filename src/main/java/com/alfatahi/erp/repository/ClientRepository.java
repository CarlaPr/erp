package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {
    List<Client> findByIsActiveTrueOrderByNameAsc();
}