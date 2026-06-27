package com.alfatahi.erp.service;

import com.alfatahi.erp.entity.Supplier;
import com.alfatahi.erp.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    public List<Supplier> listAllActive() {
        return supplierRepository.findByIsActiveTrueOrderByNameAsc();
    }

    public Supplier save(Supplier supplier) {
        return supplierRepository.save(supplier);
    }

    public Supplier findById(UUID id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Fornecedor não encontrado: " + id));
    }

    public void delete(UUID id) {
        Supplier supplier = findById(id);
        supplier.setIsActive(false);
        supplierRepository.save(supplier);
    }

}