package com.alfatahi.erp.planocorte.dto;

import java.util.ArrayList;
import java.util.List;

public class ResultadoImportacao {

    private int criados = 0;
    private int atualizados = 0;
    private final List<String> erros = new ArrayList<>();

    public void incrementarCriados() {
        this.criados++;
    }

    public void incrementarAtualizados() {
        this.atualizados++;
    }

    public int getCriados() {
        return criados;
    }

    public int getAtualizados() {
        return atualizados;
    }

    public List<String> getErros() {
        return erros;
    }

    public boolean isComErros() {
        return !erros.isEmpty();
    }

    public int getTotalProcessado() {
        return criados + atualizados;
    }
}
