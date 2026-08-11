package com.alfatahi.erp.planocorte.service;

import com.alfatahi.erp.planocorte.dto.PerfilAluminioForm;
import com.alfatahi.erp.entity.Supplier;
import com.alfatahi.erp.planocorte.entity.PerfilAluminio;
import com.alfatahi.erp.repository.SupplierRepository;
import com.alfatahi.erp.planocorte.repository.PerfilAluminioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class PerfilAluminioService {

    private final PerfilAluminioRepository perfilAluminioRepository;
    private final SupplierRepository supplierRepository;

    public PerfilAluminioService(PerfilAluminioRepository perfilAluminioRepository,
                                  SupplierRepository supplierRepository) {
        this.perfilAluminioRepository = perfilAluminioRepository;
        this.supplierRepository = supplierRepository;
    }

    @Transactional(readOnly = true)
    public List<PerfilAluminio> listarTodos() {
        return perfilAluminioRepository.findAll();
    }

    @Transactional(readOnly = true)
    public PerfilAluminio buscarPorId(Long id) {
        return perfilAluminioRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Perfil de alumínio não encontrado: " + id));
    }

    public PerfilAluminio salvar(PerfilAluminioForm form) {
        PerfilAluminio perfil = form.getId() != null ? buscarPorId(form.getId()) : new PerfilAluminio();

        perfil.setLinha(form.getLinha());
        perfil.setPerfil(form.getPerfil());
        perfil.setCor(form.getCor());
        perfil.setFabricante(form.getFabricante());
        perfil.setValorPorMetro(form.getValorPorMetro());
        perfil.setSupplier(resolverSupplier(form.getSupplierId()));

        return perfilAluminioRepository.save(perfil);
    }

    public void inativar(Long id) {
        PerfilAluminio perfil = buscarPorId(id);
        perfil.setAtivo(false);
        perfilAluminioRepository.save(perfil);
    }

    private Supplier resolverSupplier(java.util.UUID supplierId) {
        if (supplierId == null) {
            return null;
        }
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new NoSuchElementException("Supplier não encontrado: " + supplierId));
    }
}
