package com.alfatahi.erp.planocorte.service;

import com.alfatahi.erp.planocorte.dto.RegraTecnicaForm;
import com.alfatahi.erp.planocorte.entity.CategoriaServico;
import com.alfatahi.erp.planocorte.entity.DimensaoAjuste;
import com.alfatahi.erp.planocorte.entity.RegraTecnica;
import com.alfatahi.erp.planocorte.repository.RegraTecnicaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class RegraTecnicaService {

    private final RegraTecnicaRepository regraTecnicaRepository;

    public RegraTecnicaService(RegraTecnicaRepository regraTecnicaRepository) {
        this.regraTecnicaRepository = regraTecnicaRepository;
    }

    @Transactional(readOnly = true)
    public List<RegraTecnica> listarTodas() {
        return regraTecnicaRepository.findAllByOrderByCategoriaAscCodigoAsc();
    }

    @Transactional(readOnly = true)
    public RegraTecnica buscarPorId(Long id) {
        return regraTecnicaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Regra técnica não encontrada: " + id));
    }






    @Transactional(readOnly = true)
    public List<RegraTecnica> listarAtivasPorCategoria(CategoriaServico categoria) {
        return regraTecnicaRepository.findByCategoriaAndAtivoTrueOrderByCodigoAsc(categoria);
    }



    @Transactional(readOnly = true)
    public AjusteCalculado calcularAjuste(CategoriaServico categoria) {
        List<RegraTecnica> regras = regraTecnicaRepository.findByCategoriaAndAtivoTrueOrderByCodigoAsc(categoria);

        BigDecimal ajusteLargura = BigDecimal.ZERO;
        BigDecimal ajusteAltura = BigDecimal.ZERO;

        for (RegraTecnica regra : regras) {
            if (regra.getDimensao() == DimensaoAjuste.LARGURA || regra.getDimensao() == DimensaoAjuste.LARGURA_E_ALTURA) {
                ajusteLargura = ajusteLargura.add(regra.getValorMm());
            }
            if (regra.getDimensao() == DimensaoAjuste.ALTURA || regra.getDimensao() == DimensaoAjuste.LARGURA_E_ALTURA) {
                ajusteAltura = ajusteAltura.add(regra.getValorMm());
            }
        }

        return new AjusteCalculado(ajusteLargura, ajusteAltura);
    }

    public RegraTecnica salvar(RegraTecnicaForm form) {
        RegraTecnica regra = form.getId() != null ? buscarPorId(form.getId()) : new RegraTecnica();

        regra.setCategoria(form.getCategoria());
        regra.setCodigo(form.getCodigo().trim().toUpperCase().replace(" ", "_"));
        regra.setDescricao(form.getDescricao());
        regra.setDimensao(form.getDimensao());
        regra.setValorMm(form.getValorMm());

        return regraTecnicaRepository.save(regra);
    }

    public void excluir(Long id) {
        regraTecnicaRepository.deleteById(id);
    }

    public record AjusteCalculado(BigDecimal largura, BigDecimal altura) {
    }
}
