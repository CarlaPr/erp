package com.alfatahi.erp.planocorte.calculo;

import com.alfatahi.erp.planocorte.entity.CategoriaServico;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ServicoCalculadoraRegistry {

    private final Map<CategoriaServico, ServicoCalculadora> porCategoria;

    public ServicoCalculadoraRegistry(List<ServicoCalculadora> calculadoras) {
        this.porCategoria = calculadoras.stream()
                .collect(Collectors.toMap(ServicoCalculadora::getCategoria, Function.identity()));
    }

    public Optional<ServicoCalculadora> buscar(CategoriaServico categoria) {
        return Optional.ofNullable(porCategoria.get(categoria));
    }

    public boolean possuiCalculadora(CategoriaServico categoria) {
        return porCategoria.containsKey(categoria);
    }
}
