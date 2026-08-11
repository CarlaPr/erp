package com.alfatahi.erp.planocorte.service;

import com.alfatahi.erp.planocorte.dto.AcessorioForm;
import com.alfatahi.erp.planocorte.entity.Acessorio;
import com.alfatahi.erp.entity.Supplier;
import com.alfatahi.erp.planocorte.repository.AcessorioRepository;
import com.alfatahi.erp.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class AcessorioService {

    private final AcessorioRepository acessorioRepository;
    private final SupplierRepository supplierRepository;

    public AcessorioService(AcessorioRepository acessorioRepository, SupplierRepository supplierRepository) {
        this.acessorioRepository = acessorioRepository;
        this.supplierRepository = supplierRepository;
    }

    @Transactional(readOnly = true)
    public List<Acessorio> listarTodos() {
        return acessorioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Acessorio buscarPorId(Long id) {
        return acessorioRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Acessório não encontrado: " + id));
    }

    public Acessorio salvar(AcessorioForm form) {
        Acessorio acessorio = form.getId() != null ? buscarPorId(form.getId()) : new Acessorio();

        acessorio.setNome(form.getNome());
        acessorio.setTipo(form.getTipo());
        acessorio.setUnidade(form.getUnidade() != null && !form.getUnidade().isBlank() ? form.getUnidade() : "un");
        acessorio.setValor(form.getValor());
        acessorio.setSupplier(resolverSupplier(form.getSupplierId()));

        return acessorioRepository.save(acessorio);
    }

    public void inativar(Long id) {
        Acessorio acessorio = buscarPorId(id);
        acessorio.setAtivo(false);
        acessorioRepository.save(acessorio);
    }

    private Supplier resolverSupplier(java.util.UUID supplierId) {
        if (supplierId == null) {
            return null;
        }
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new NoSuchElementException("Supplier não encontrado: " + supplierId));
    }
}
