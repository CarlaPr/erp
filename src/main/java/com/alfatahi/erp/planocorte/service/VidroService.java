package com.alfatahi.erp.planocorte.service;

import com.alfatahi.erp.planocorte.dto.VidroForm;
import com.alfatahi.erp.entity.Supplier;
import com.alfatahi.erp.planocorte.entity.HistoricoPrecoVidro;
import com.alfatahi.erp.planocorte.entity.OrigemHistorico;
import com.alfatahi.erp.planocorte.entity.Vidro;
import com.alfatahi.erp.repository.SupplierRepository;
import com.alfatahi.erp.planocorte.repository.HistoricoPrecoVidroRepository;
import com.alfatahi.erp.planocorte.repository.VidroRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class VidroService {

    private final VidroRepository vidroRepository;
    private final SupplierRepository supplierRepository;
    private final HistoricoPrecoVidroRepository historicoRepository;

    public VidroService(VidroRepository vidroRepository,
                         SupplierRepository supplierRepository,
                         HistoricoPrecoVidroRepository historicoRepository) {
        this.vidroRepository = vidroRepository;
        this.supplierRepository = supplierRepository;
        this.historicoRepository = historicoRepository;
    }

    @Transactional(readOnly = true)
    public List<Vidro> listarAtivos() {
        return vidroRepository.findByAtivoTrueOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public List<Vidro> listarTodos() {
        return vidroRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Vidro buscarPorId(Long id) {
        return vidroRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Vidro nao encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public List<HistoricoPrecoVidro> historico(Long vidroId) {
        return historicoRepository.findByVidroIdOrderByCriadoEmDesc(vidroId);
    }

    public Vidro salvar(VidroForm form) {
        boolean novo = form.getId() == null;
        Vidro vidro = novo ? new Vidro() : buscarPorId(form.getId());
        BigDecimal valorAntigo = vidro.getValorPorM2();

        vidro.setNome(form.getNome());
        vidro.setFabricante(form.getFabricante());

        if (form.getSupplierId() != null) {
            Supplier supplier = supplierRepository.findById(form.getSupplierId())
                    .orElseThrow(() -> new NoSuchElementException(
                            "Supplier nao encontrado: " + form.getSupplierId()));
            vidro.setSupplier(supplier);
        } else {
            vidro.setSupplier(null);
        }

        vidro.setTipo(form.getTipo());
        vidro.setEspessura(form.getEspessura());
        vidro.setCor(form.getCor());
        vidro.setAcabamento(form.getAcabamento());
        vidro.setValorPorM2(form.getValorPorM2());
        vidro.setValorMinimo(form.getValorMinimo());
        vidro.setPesoPorM2(form.getPesoPorM2());
        vidro.setDataVigencia(form.getDataVigencia());
        vidro.setDataValidade(form.getDataValidade());
        vidro.setObservacoes(form.getObservacoes());

        Vidro salvo = vidroRepository.save(vidro);

        if (novo || valorAntigo == null || valorAntigo.compareTo(salvo.getValorPorM2()) != 0) {
            registrarHistorico(salvo, valorAntigo, salvo.getValorPorM2(), OrigemHistorico.MANUAL);
        }

        return salvo;
    }

    public void inativar(Long id) {
        Vidro vidro = buscarPorId(id);
        vidro.setAtivo(false);
        vidroRepository.save(vidro);
    }

    void registrarHistorico(Vidro vidro, BigDecimal valorAntigo, BigDecimal valorNovo, OrigemHistorico origem) {
        HistoricoPrecoVidro historico = new HistoricoPrecoVidro();
        historico.setVidroId(vidro.getId());
        historico.setVidroNomeSnapshot(vidro.getNome());
        historico.setValorAntigo(valorAntigo);
        historico.setValorNovo(valorNovo);
        historico.setUsuario(usuarioAtual());
        historico.setOrigem(origem);
        historicoRepository.save(historico);
    }

    private String usuarioAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "sistema";
    }
}
