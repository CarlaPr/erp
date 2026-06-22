package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {
    List<Supplier> findByIsActiveTrueOrderByNameAsc();
}