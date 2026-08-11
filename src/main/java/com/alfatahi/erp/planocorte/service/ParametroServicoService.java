package com.alfatahi.erp.planocorte.service;

import com.alfatahi.erp.planocorte.dto.ParametroServicoForm;
import com.alfatahi.erp.planocorte.entity.CategoriaServico;
import com.alfatahi.erp.planocorte.entity.ParametroServico;
import com.alfatahi.erp.planocorte.repository.ParametroServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional
public class ParametroServicoService {

    private final ParametroServicoRepository parametroServicoRepository;

    public ParametroServicoService(ParametroServicoRepository parametroServicoRepository) {
        this.parametroServicoRepository = parametroServicoRepository;
    }

    @Transactional(readOnly = true)
    public List<ParametroServico> listarTodos() {
        return parametroServicoRepository.findAllByOrderByCategoriaAscCodigoAsc();
    }

    @Transactional(readOnly = true)
    public ParametroServico buscarPorId(Long id) {
        return parametroServicoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Parâmetro não encontrado: " + id));
    }



    @Transactional(readOnly = true)
    public BigDecimal valor(CategoriaServico categoria, String codigo, BigDecimal valorPadrao) {
        return parametroServicoRepository.findByCategoriaAndCodigoAndAtivoTrue(categoria, codigo)
                .map(ParametroServico::getValor)
                .orElse(valorPadrao);
    }

    @Transactional(readOnly = true)
    public String valorTexto(CategoriaServico categoria, String codigo, String valorPadrao) {
        return parametroServicoRepository.findByCategoriaAndCodigoAndAtivoTrue(categoria, codigo)
                .map(ParametroServico::getValorTexto)
                .orElse(valorPadrao);
    }

    public ParametroServico salvar(ParametroServicoForm form) {
        ParametroServico parametro = form.getId() != null ? buscarPorId(form.getId()) : new ParametroServico();

        parametro.setCategoria(form.getCategoria());
        parametro.setCodigo(form.getCodigo().trim().toUpperCase().replace(" ", "_"));
        parametro.setDescricao(form.getDescricao());
        parametro.setValor(form.getValor());
        parametro.setValorTexto(form.getValorTexto());
        parametro.setOrigem(form.getOrigem());

        return parametroServicoRepository.save(parametro);
    }

    public void excluir(Long id) {
        parametroServicoRepository.deleteById(id);
    }
}
