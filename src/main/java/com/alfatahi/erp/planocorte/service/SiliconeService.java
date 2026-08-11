package com.alfatahi.erp.planocorte.service;

import com.alfatahi.erp.planocorte.dto.SiliconeForm;
import com.alfatahi.erp.entity.Supplier;
import com.alfatahi.erp.planocorte.entity.Silicone;
import com.alfatahi.erp.repository.SupplierRepository;
import com.alfatahi.erp.planocorte.repository.SiliconeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class SiliconeService {

    private final SiliconeRepository siliconeRepository;
    private final SupplierRepository supplierRepository;

    public SiliconeService(SiliconeRepository siliconeRepository, SupplierRepository supplierRepository) {
        this.siliconeRepository = siliconeRepository;
        this.supplierRepository = supplierRepository;
    }

    @Transactional(readOnly = true)
    public List<Silicone> listarTodos() {
        return siliconeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Silicone buscarPorId(Long id) {
        return siliconeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Silicone não encontrado: " + id));
    }

    public Silicone salvar(SiliconeForm form) {
        Silicone silicone = form.getId() != null ? buscarPorId(form.getId()) : new Silicone();

        silicone.setMarca(form.getMarca());
        silicone.setCor(form.getCor());
        silicone.setEmbalagem(form.getEmbalagem());
        silicone.setValor(form.getValor());
        silicone.setSupplier(resolverSupplier(form.getSupplierId()));

        return siliconeRepository.save(silicone);
    }

    public void inativar(Long id) {
        Silicone silicone = buscarPorId(id);
        silicone.setAtivo(false);
        siliconeRepository.save(silicone);
    }

    private Supplier resolverSupplier(java.util.UUID supplierId) {
        if (supplierId == null) {
            return null;
        }
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new NoSuchElementException("Supplier não encontrado: " + supplierId));
    }
}
