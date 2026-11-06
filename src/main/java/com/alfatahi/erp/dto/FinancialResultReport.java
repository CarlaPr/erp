package com.alfatahi.erp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record FinancialResultReport(FinancialResultFilter filter, CashSummary cash, CashSummary previous,
                                     List<OrderResult> orders, List<Movement> movements, CrossPeriod crossPeriod,
                                     List<ResultLine> operating, List<String> warnings) {
    public record CashSummary(BigDecimal opening, BigDecimal incoming, BigDecimal outgoing, long count) {
        public BigDecimal net() { return incoming.subtract(outgoing); }
        public BigDecimal closing() { return opening.add(net()); }
    }
    public record Option(String value, String label) { }
    public record Movement(UUID id, @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate date, String description, String category, String party, String type,
                           BigDecimal incoming, BigDecimal outgoing, String paymentMethod, String sourceUrl,
                           List<Option> orders, boolean allocated, String reconciliation) {
        public BigDecimal amount() { return incoming.add(outgoing); }
    }
    public record OrderResult(UUID id, String number, String client, String description, LocalDate saleDate,
                              String status, BigDecimal sale, BigDecimal received, BigDecimal receivedPeriod,
                              BigDecimal receivedBefore, BigDecimal receivable, BigDecimal materials,
                              BigDecimal labor, BigDecimal otherCosts, BigDecimal totalCosts,
                              BigDecimal paid, BigDecimal paidPeriod, BigDecimal paidBefore, BigDecimal payable,
                              BigDecimal profit, BigDecimal margin, BigDecimal cashResult, LocalDate lastMovement,
                              BigDecimal directPaidPeriod,
                              boolean soldInPeriod, boolean spansMonths, List<String> situations,
                              List<String> warnings, List<CostDetail> costDetails) {
        public boolean reliable() { return profit != null; }
        public boolean pendingReceipt() { return receivable.signum() > 0; }
        public boolean pendingPayment() { return payable.signum() > 0; }
        public String statusLabel() {
            return switch (status == null ? "" : status) {
                case "in_progress" -> "Em andamento";
                case "completed", "done", "delivered", "concluida" -> "Concluída";
                case "pending" -> "Pendente";
                default -> status == null ? "Não informado" : status;
            };
        }
    }
    public record CostDetail(String description, String category, LocalDate dueDate, LocalDate paymentDate,
                             String status, BigDecimal amount, BigDecimal paid, BigDecimal pending,
                             boolean directCost, boolean manual, boolean invalid) { }
    public record CrossPeriod(List<OrderResult> orders, BigDecimal receiptsOldSales, BigDecimal receivablesNewSales,
                              BigDecimal paymentsOldSales, BigDecimal pendingCostsInProgress) { }
    public record ResultLine(String label, BigDecimal amount, boolean result, boolean outside) { }
}
