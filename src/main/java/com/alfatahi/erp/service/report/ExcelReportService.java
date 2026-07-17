package com.alfatahi.erp.service.report;

import com.alfatahi.erp.dto.CashLedgerEntryDto;
import com.alfatahi.erp.dto.DreReportDto;
import com.alfatahi.erp.dto.WorkOrderItemReportDto;
import com.alfatahi.erp.dto.WorkOrderReportDto;
import com.alfatahi.erp.dto.WorkOrderReportTotalsDto;
import com.alfatahi.erp.entity.AccountsPayable;
import com.alfatahi.erp.entity.AccountsReceivable;
import com.alfatahi.erp.entity.Client;
import com.alfatahi.erp.entity.Supplier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.alfatahi.erp.service.report.ExcelSheetBuilder.PercentValue;

@Service
public class ExcelReportService {

    // ── Ordens de Serviço ────────────────────────────────────────────────

    public byte[] workOrderIndividual(WorkOrderReportDto os) {
        ExcelSheetBuilder xls = new ExcelSheetBuilder("OS " + safe(os.getNumber()));
        xls.title("Relatório da Ordem de Serviço " + safe(os.getNumber()), "Cliente: " + safe(os.getClientName()));

        xls.header("Campo", "Valor");
        xls.row("Número", os.getNumber());
        xls.row("Título", os.getTitle());
        xls.row("Status", os.getStatusLabel());
        xls.row("Categoria", os.getCategoryName());
        xls.row("Cliente", os.getClientName());
        xls.row("Documento do Cliente", os.getClientDocument());
        xls.row("Data de Criação", os.getCreatedAt());
        xls.row("Data de Instalação", os.getInstallDate());
        xls.blankRow();

        xls.header("Item / Material", "Qtd.", "Custo Unit.", "Preço Unit.", "Custo Total", "Receita Total", "Lucro", "Margem %");
        for (WorkOrderItemReportDto item : os.getItems()) {
            xls.row(item.getDescription(), item.getQuantity(), item.getUnitCost(), item.getUnitPrice(),
                    item.getTotalCost(), item.getTotalPrice(), item.getProfit(), PercentValue.of(item.getMarginPercent()));
        }
        xls.totalsRow("TOTAL", "", "", "", os.getTotalCost(), os.getTotalRevenue(), os.getProfit(), PercentValue.of(os.getMarginPercent()));

        return xls.build();
    }

    public byte[] workOrdersConsolidated(List<WorkOrderReportDto> list, WorkOrderReportTotalsDto totals,
                                          boolean detailed, String subtitle) {
        ExcelSheetBuilder xls = new ExcelSheetBuilder("Ordens de Servico");
        xls.title("Relatório Consolidado de Ordens de Serviço", subtitle);

        xls.header("Número", "Cliente", "Status", "Data", "Receita", "Custo", "Lucro", "Margem %");
        for (WorkOrderReportDto os : list) {
            xls.row(os.getNumber(), os.getClientName(), os.getStatusLabel(), os.getCreatedAt(),
                    os.getTotalRevenue(), os.getTotalCost(), os.getProfit(), PercentValue.of(os.getMarginPercent()));
        }
        xls.totalsRow("TOTAL (" + totals.getQuantity() + " O.S.)", "", "", "",
                totals.getTotalRevenue(), totals.getTotalCost(), totals.getTotalProfit(), PercentValue.of(totals.getAverageMargin()));

        if (detailed) {
            xls.blankRow();
            xls.blankRow();
            xls.title("Detalhamento por Item de cada O.S.", null);
            xls.header("O.S.", "Cliente", "Item / Material", "Qtd.", "Custo Unit.", "Preço Unit.", "Custo Total", "Receita Total", "Lucro");
            for (WorkOrderReportDto os : list) {
                for (WorkOrderItemReportDto item : os.getItems()) {
                    xls.row(os.getNumber(), os.getClientName(), item.getDescription(), item.getQuantity(),
                            item.getUnitCost(), item.getUnitPrice(), item.getTotalCost(), item.getTotalPrice(), item.getProfit());
                }
            }
        }

        return xls.build();
    }

    // ── Financeiro ───────────────────────────────────────────────────────

    public byte[] receivables(List<AccountsReceivable> list, String subtitle) {
        ExcelSheetBuilder xls = new ExcelSheetBuilder("Contas a Receber");
        xls.title("Relatório de Contas a Receber", subtitle);
        xls.header("Cliente", "Descrição", "O.S.", "Vencimento", "Pagamento", "Status", "Valor Total", "Valor Recebido", "Saldo");

        BigDecimal totalValor = BigDecimal.ZERO, totalRecebido = BigDecimal.ZERO, totalSaldo = BigDecimal.ZERO;
        for (AccountsReceivable r : list) {
            xls.row(r.getClient() != null ? r.getClient().getName() : "Consumidor Final",
                    r.getDescription(),
                    r.getWorkOrder() != null ? r.getWorkOrder().getNumber() : "—",
                    r.getDueDate(), r.getPaymentDate(), statusLabel(r.getStatus()),
                    r.getTotalAmount(), r.getReceivedAmount(), r.getBalance());
            totalValor = totalValor.add(nvl(r.getTotalAmount()));
            totalRecebido = totalRecebido.add(nvl(r.getReceivedAmount()));
            totalSaldo = totalSaldo.add(nvl(r.getBalance()));
        }
        xls.totalsRow("TOTAL (" + list.size() + ")", "", "", "", "", "", totalValor, totalRecebido, totalSaldo);
        return xls.build();
    }

    public byte[] payables(List<AccountsPayable> list, String subtitle) {
        ExcelSheetBuilder xls = new ExcelSheetBuilder("Contas a Pagar");
        xls.title("Relatório de Contas a Pagar", subtitle);
        xls.header("Fornecedor", "Descrição", "Categoria", "O.S.", "Vencimento", "Pagamento", "Status", "Valor Total", "Valor Pago", "Saldo");

        BigDecimal totalValor = BigDecimal.ZERO, totalPago = BigDecimal.ZERO, totalSaldo = BigDecimal.ZERO;
        for (AccountsPayable p : list) {
            xls.row(p.getSupplier() != null ? p.getSupplier().getName() : "—",
                    p.getDescription(), p.getCategory(),
                    p.getWorkOrder() != null ? p.getWorkOrder().getNumber() : "—",
                    p.getDueDate(), p.getPaymentDate(), statusLabel(p.getStatus()),
                    p.getTotalAmount(), p.getPaidAmount(), p.getBalance());
            totalValor = totalValor.add(nvl(p.getTotalAmount()));
            totalPago = totalPago.add(nvl(p.getPaidAmount()));
            totalSaldo = totalSaldo.add(nvl(p.getBalance()));
        }
        xls.totalsRow("TOTAL (" + list.size() + ")", "", "", "", "", "", "", totalValor, totalPago, totalSaldo);
        return xls.build();
    }

    public byte[] cashLedger(List<CashLedgerEntryDto> entries, BigDecimal openingBalance,
                              BigDecimal totalEntradas, BigDecimal totalSaidas, BigDecimal saldoFinal, String subtitle) {
        ExcelSheetBuilder xls = new ExcelSheetBuilder("Fluxo de Caixa");
        xls.title("Livro Caixa / Fluxo de Caixa", subtitle);
        xls.header("Data", "Tipo", "Descrição", "Cliente/Fornecedor", "O.S.", "Forma Pgto.", "Entrada", "Saída", "Saldo");
        xls.row("Saldo Inicial", "", "", "", "", "", "", "", openingBalance);
        for (CashLedgerEntryDto e : entries) {
            xls.row(e.getDate(), e.getType() == CashLedgerEntryDto.EntryType.ENTRADA ? "Entrada" : "Saída",
                    e.getDescription(), e.getParty(), e.getWorkOrderNumber(), e.getPaymentMethod(),
                    e.getEntrada(), e.getSaida(), e.getSaldo());
        }
        xls.totalsRow("TOTAL", "", "", "", "", "", totalEntradas, totalSaidas, saldoFinal);
        return xls.build();
    }

    public byte[] dre(List<DreReportDto> columns) {
        ExcelSheetBuilder xls = new ExcelSheetBuilder("DRE");
        xls.title("DRE Gerencial", null);
        xls.header("Período", "Receita Bruta", "Impostos", "Receita Líquida", "CMV", "Lucro Bruto",
                "Despesas Fixas", "Desp. Financeiras", "Lucro Líquido", "Margem %");
        for (DreReportDto d : columns) {
            xls.row(d.getMesAno(), d.getReceitaBruta(), d.getImpostos(), d.getReceitaLiquida(), d.getCmv(),
                    d.getLucroBruto(), d.getDespesasFixas(), d.getDespesasFinanceiras(), d.getLucroLiquido(),
                    PercentValue.of(d.getMargemLiquida()));
        }
        return xls.build();
    }

    // ── Cadastros ────────────────────────────────────────────────────────

    public byte[] clients(List<Client> list) {
        ExcelSheetBuilder xls = new ExcelSheetBuilder("Clientes");
        xls.title("Relatório de Clientes", "Total: " + list.size());
        xls.header("Nome", "Tipo", "Documento", "Telefone", "E-mail", "Cidade", "Endereço", "Ativo", "Cadastrado em");
        for (Client c : list) {
            xls.row(c.getName(), "company".equals(c.getType()) ? "Pessoa Jurídica" : "Pessoa Física",
                    c.getDocument(), c.getPhone(), c.getEmail(), c.getCity(), c.getAddress(),
                    Boolean.TRUE.equals(c.getIsActive()) ? "Sim" : "Não", c.getCreatedAt());
        }
        return xls.build();
    }

    public byte[] suppliers(List<Supplier> list) {
        ExcelSheetBuilder xls = new ExcelSheetBuilder("Fornecedores");
        xls.title("Relatório de Fornecedores", "Total: " + list.size());
        xls.header("Nome", "Categoria", "Documento", "Telefone", "E-mail", "Cidade", "Ativo", "Cadastrado em");
        for (Supplier s : list) {
            xls.row(s.getName(), s.getCategory(), s.getDocument(), s.getPhone(), s.getEmail(), s.getCity(),
                    Boolean.TRUE.equals(s.getIsActive()) ? "Sim" : "Não", s.getCreatedAt());
        }
        return xls.build();
    }

    // ── Contador ─────────────────────────────────────────────────────────

    public byte[] accountant(List<DreReportDto> dreColumns, List<AccountsPayable> payablesPeriod,
                              List<AccountsReceivable> receivablesPeriod, String subtitle) {
        ExcelSheetBuilder xls = new ExcelSheetBuilder("Relatorio Contabil");
        xls.title("Relatório para o Contador", subtitle);

        xls.header("Período", "Receita Bruta", "Impostos Estimados", "CMV", "Despesas Fixas",
                "Despesas Financeiras", "Lucro Líquido", "Margem %");
        for (DreReportDto d : dreColumns) {
            xls.row(d.getMesAno(), d.getReceitaBruta(), d.getImpostos(), d.getCmv(), d.getDespesasFixas(),
                    d.getDespesasFinanceiras(), d.getLucroLiquido(), PercentValue.of(d.getMargemLiquida()));
        }

        xls.blankRow();
        xls.blankRow();
        xls.title("Recebimentos no Período (Contas a Receber baixadas)", null);
        xls.header("Cliente", "Descrição", "Data Pagamento", "Forma Pgto.", "Valor Recebido");
        BigDecimal totalRec = BigDecimal.ZERO;
        for (AccountsReceivable r : receivablesPeriod) {
            xls.row(r.getClient() != null ? r.getClient().getName() : "Consumidor Final",
                    r.getDescription(), r.getPaymentDate(), r.getPaymentMethod(), r.getReceivedAmount());
            totalRec = totalRec.add(nvl(r.getReceivedAmount()));
        }
        xls.totalsRow("TOTAL RECEBIDO", "", "", "", totalRec);

        xls.blankRow();
        xls.blankRow();
        xls.title("Pagamentos no Período (Contas a Pagar baixadas) por Categoria", null);
        xls.header("Fornecedor", "Descrição", "Categoria/Subcategoria", "Data Pagamento", "Forma Pgto.", "Valor Pago");
        BigDecimal totalPag = BigDecimal.ZERO;
        for (AccountsPayable p : payablesPeriod) {
            String cat = safe(p.getCategory()) + (p.getSubcategory() != null && !p.getSubcategory().isBlank() ? " / " + p.getSubcategory() : "");
            xls.row(p.getSupplier() != null ? p.getSupplier().getName() : "—",
                    p.getDescription(), cat, p.getPaymentDate(), p.getPaymentMethod(), p.getPaidAmount());
            totalPag = totalPag.add(nvl(p.getPaidAmount()));
        }
        xls.totalsRow("TOTAL PAGO", "", "", "", "", totalPag);

        return xls.build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────
    private BigDecimal nvl(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
    private String safe(String s) { return s != null ? s : ""; }

    private String statusLabel(String status) {
        if (status == null) return "—";
        return switch (status) {
            case "pending" -> "Pendente";
            case "partial" -> "Parcial";
            case "received" -> "Recebido";
            case "paid" -> "Pago";
            case "cancelled" -> "Cancelado";
            default -> status;
        };
    }
}
