package com.alfatahi.erp.service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum DreGroup {

    DED_IMPOSTOS_VENDAS("Impostos sobre vendas (DAS/Simples, ISS, ICMS, PIS, COFINS)", Section.DEDUCOES),

    CMV_MATERIAIS("Materiais e insumos (vidros, ferragens, alumínio, vedação)", Section.CUSTOS),
    CMV_SERV_TERCEIROS("Serviços de produção terceirizados (têmpera, lapidação, furação...)", Section.CUSTOS),
    CMV_INSTALACAO("Instalação, mão de obra e logística de obra", Section.CUSTOS),

    DESP_PESSOAL("Pessoal e encargos", Section.OPERACIONAIS),
    DESP_COMERCIAL("Comerciais e marketing", Section.OPERACIONAIS),
    DESP_OCUPACAO("Ocupação e utilidades (aluguel, energia, internet...)", Section.OPERACIONAIS),
    DESP_ADMIN("Administrativas e serviços (contabilidade, sistemas, escritório...)", Section.OPERACIONAIS),
    DESP_MANUTENCAO("Manutenção e conservação", Section.OPERACIONAIS),
    DESP_TRIBUTARIAS("Impostos e taxas gerais (IPTU, IPVA, taxas)", Section.OPERACIONAIS),
    DESP_OUTRAS("Outras despesas operacionais", Section.OPERACIONAIS),

    DESP_FIN_CARTAO("Taxas de cartão / maquininha", Section.FINANCEIRO),
    DESP_FINANCEIRAS("Juros, tarifas bancárias, multas e IOF", Section.FINANCEIRO),

    IR_CSLL("IRPJ e CSLL (imposto sobre o lucro)", Section.IMPOSTO_LUCRO),

    FORA_INVESTIMENTOS("Investimentos / compra de bens duráveis", Section.FORA_DRE),
    FORA_AMORTIZACAO("Amortização de empréstimos e financiamentos (principal)", Section.FORA_DRE),
    FORA_RESERVAS("Reservas e provisionamento de caixa", Section.FORA_DRE),
    FORA_RETIRADAS("Retiradas e distribuição de lucros", Section.FORA_DRE),

    NAO_CLASSIFICADO("Não classificado (revisar)", Section.NAO_CLASSIFICADO);

    public enum Section {
        DEDUCOES("Deduções da receita"),
        CUSTOS("Custo dos serviços (CMV/CSP)"),
        OPERACIONAIS("Despesas operacionais"),
        FINANCEIRO("Resultado financeiro"),
        IMPOSTO_LUCRO("Imposto sobre o lucro"),
        FORA_DRE("Fora da DRE (não afeta o resultado)"),
        NAO_CLASSIFICADO("Sem classificação");

        private final String label;

        Section(String label) { this.label = label; }

        public String getLabel() { return label; }
    }

    private final String label;
    private final Section section;

    DreGroup(String label, Section section) {
        this.label = label;
        this.section = section;
    }

    public String getKey() { return name(); }
    public String getLabel() { return label; }
    public Section getSection() { return section; }
    public boolean isOutsideResult() { return section == Section.FORA_DRE; }

    public static DreGroup fromKey(String key) {
        if (key == null || key.isBlank()) return NAO_CLASSIFICADO;
        String normalized = key.strip().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(group -> group.name().equals(normalized))
                .findFirst()
                .orElse(NAO_CLASSIFICADO);
    }

    public static boolean isValidKey(String key) {
        if (key == null || key.isBlank()) return false;
        String normalized = key.strip().toUpperCase(Locale.ROOT);
        return Arrays.stream(values()).anyMatch(group -> group.name().equals(normalized));
    }

    public static List<DreGroup> selectable() {
        return List.of(values());
    }
}
