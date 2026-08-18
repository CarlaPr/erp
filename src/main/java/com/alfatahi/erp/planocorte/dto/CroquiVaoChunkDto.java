package com.alfatahi.erp.planocorte.dto;

/**
 * Representa um "pedaço" (chunk) do croqui combinado de um vão.
 * Quando um vão tem mais folhas do que o máximo permitido por croqui,
 * as folhas são divididas em vários chunks para melhor visualização,
 * cada um com seu próprio SVG combinado.
 */
public class CroquiVaoChunkDto {

    private final String svg;
    private final int folhaInicio;
    private final int folhaFim;
    private final int totalFolhas;

    public CroquiVaoChunkDto(String svg, int folhaInicio, int folhaFim, int totalFolhas) {
        this.svg = svg;
        this.folhaInicio = folhaInicio;
        this.folhaFim = folhaFim;
        this.totalFolhas = totalFolhas;
    }

    public String getSvg() {
        return svg;
    }

    public int getFolhaInicio() {
        return folhaInicio;
    }

    public int getFolhaFim() {
        return folhaFim;
    }

    public int getTotalFolhas() {
        return totalFolhas;
    }

    /**
     * true quando este chunk sozinho já representa todas as folhas do vão
     * (ou seja, o vão não precisou ser dividido em mais de um croqui).
     */
    public boolean isChunkUnico() {
        return folhaInicio == 1 && folhaFim == totalFolhas;
    }
}
