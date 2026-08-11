package com.alfatahi.erp.planocorte.service;

import com.alfatahi.erp.planocorte.dto.FerragemForm;
import com.alfatahi.erp.planocorte.entity.Ferragem;
import com.alfatahi.erp.entity.Supplier;
import com.alfatahi.erp.planocorte.repository.FerragemRepository;
import com.alfatahi.erp.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class FerragemService {

    private final FerragemRepository ferragemRepository;
    private final SupplierRepository supplierRepository;

    public FerragemService(FerragemRepository ferragemRepository, SupplierRepository supplierRepository) {
        this.ferragemRepository = ferragemRepository;
        this.supplierRepository = supplierRepository;
    }

    @Transactional(readOnly = true)
    public List<Ferragem> listarTodas() {
        return ferragemRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Ferragem buscarPorId(Long id) {
        return ferragemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Ferragem não encontrada: " + id));
    }

    public Ferragem salvar(FerragemForm form) {
        Ferragem ferragem = form.getId() != null ? buscarPorId(form.getId()) : new Ferragem();

        ferragem.setCategoria(form.getCategoria());
        ferragem.setProduto(form.getProduto());
        ferragem.setCodigo(form.getCodigo());
        ferragem.setValor(form.getValor());
        ferragem.setUnidade(form.getUnidade() != null && !form.getUnidade().isBlank() ? form.getUnidade() : "un");
        ferragem.setSupplier(resolverSupplier(form.getSupplierId()));
        ferragem.setTipoElementoPadrao(form.getTipoElementoPadrao());
        ferragem.setDiametroFuracaoMm(form.getDiametroFuracaoMm());
        ferragem.setQuantidadeFuros(form.getQuantidadeFuros());
        ferragem.setDistanciaBordaMm(form.getDistanciaBordaMm());
        ferragem.setDistanciaTopoMm(form.getDistanciaTopoMm());
        ferragem.setLarguraMm(form.getLarguraMm());
        ferragem.setAlturaMm(form.getAlturaMm());
        ferragem.setComprimentoMm(form.getComprimentoMm());
        ferragem.setProfundidadeMm(form.getProfundidadeMm());
        ferragem.setRaioMm(form.getRaioMm());
        ferragem.setAnguloGraus(form.getAnguloGraus());

        return ferragemRepository.save(ferragem);
    }

    public void inativar(Long id) {
        Ferragem ferragem = buscarPorId(id);
        ferragem.setAtivo(false);
        ferragemRepository.save(ferragem);
    }

    private Supplier resolverSupplier(java.util.UUID supplierId) {
        if (supplierId == null) {
            return null;
        }
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new NoSuchElementException("Supplier não encontrado: " + supplierId));
    }
}
