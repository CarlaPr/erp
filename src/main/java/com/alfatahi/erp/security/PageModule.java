package com.alfatahi.erp.security;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Pages that can receive an individual permission. Settings stays management-only. */
public enum PageModule {
    DASHBOARD("dashboard", "Dashboard Administrativo", false, Set.of(), "/dashboard", "/dre"),
    COMMERCIAL("commercial", "Dashboard Vendas", false, Set.of("VENDAS"), "/commercial"),
    QUOTES("quotes", "Orçamentos", true, Set.of("VENDAS"), "/quotes"),
    RECEIPTS("receipts", "Recibos", true, Set.of("VENDAS"), "/receipts"),
    AGENDA("agenda", "Agenda Comercial", true, Set.of("VENDAS", "TECNICO"), "/agenda"),
    TECHNICAL_VISITS("technical-visits", "Visitas Técnicas", true, Set.of("VENDAS", "TECNICO"), "/technical-visits"),
    CLIENTS("clients", "Clientes", true, Set.of("VENDAS"), "/clients"),
    CUT_RULES("cut-plans-regras", "Regras Técnicas", true, Set.of(), "/cut-plans/regras-tecnicas"),
    CUT_PARAMETERS("cut-plans-parametros", "Parâmetros de Serviço", true, Set.of(), "/cut-plans/parametros-servico"),
    CUT_CATALOG("cut-plans-catalogo", "Catálogo de Insumos", true, Set.of("VENDAS"), "/cut-plans/catalogo"),
    CUT_PLANS("cut-plans", "Plano de Corte", true, Set.of("VENDAS"), "/cut-plans"),
    WORK_ORDERS("work-orders", "Ordens de Serviço", true, Set.of(), "/work-orders"),
    RECEIVABLES("receivables", "Contas a Receber", true, Set.of(), "/receivables"),
    PAYABLES("payables", "Contas a Pagar e Contas Fixas", true, Set.of(), "/payables"),
    LOSSES("losses", "Perdas e Prejuízos", true, Set.of(), "/losses"),
    CASH_LEDGER("cash-ledger", "Livro Caixa", false, Set.of(), "/cash-ledger"),
    FINANCIAL_CLOSING("financial-closing", "Fechamento Mensal", true, Set.of(), "/financial-closing"),
    SUPPLIERS("suppliers", "Fornecedores", true, Set.of(), "/suppliers");

    private final String key;
    private final String label;
    private final boolean writable;
    private final Set<String> defaultRoles;
    private final List<String> paths;

    PageModule(String key, String label, boolean writable, Set<String> defaultRoles, String... paths) {
        this.key = key;
        this.label = label;
        this.writable = writable;
        this.defaultRoles = defaultRoles;
        this.paths = List.of(paths);
    }

    public String getKey() { return key; }
    public String getLabel() { return label; }
    public String getPath() { return paths.getFirst(); }
    public boolean isWritable() { return writable; }
    public boolean allowedByRole(String role) { return "GESTAO".equals(role) || defaultRoles.contains(role); }

    public static Optional<PageModule> fromKey(String key) {
        return Arrays.stream(values()).filter(page -> page.key.equals(key)).findFirst();
    }

    public static Optional<PageModule> fromPath(String path) {
        if (path.equals("/agenda/technical-visits") || path.startsWith("/agenda/technical-visits/")
                || path.matches("/quotes/[^/]+/technical-visits")) return Optional.of(TECHNICAL_VISITS);
        return Arrays.stream(values()).filter(page -> page.paths.stream()
                .anyMatch(prefix -> path.equals(prefix) || path.startsWith(prefix + "/"))).findFirst();
    }
}
