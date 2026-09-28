package com.alfatahi.erp.repository;

import com.alfatahi.erp.dto.DashboardMonthDto;
import com.alfatahi.erp.dto.DashboardPaymentMethodDto;
import com.alfatahi.erp.service.FinancialPeriod;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;

@Repository
public class DashboardRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public DashboardRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String ACTIVE_MOVEMENTS = """
            FROM financial_movements m
            LEFT JOIN accounts_receivable r ON r.id = m.accounts_receivable_id
            LEFT JOIN accounts_payable p ON p.id = m.accounts_payable_id
            WHERE (r.status IN ('received', 'partial') OR p.status IN ('paid', 'partial'))
            """;

    public Map<String, Object> periodTotals(FinancialPeriod period, LocalDate today) {
        var params = new MapSqlParameterSource()
                .addValue("start", period.from()).addValue("end", period.endExclusive())
                .addValue("today", today)
                .addValue("overdue", today.isBefore(period.endExclusive()) ? today : period.endExclusive());
        Map<String, Object> totals = new HashMap<>();
        totals.putAll(jdbc.queryForMap("""
                SELECT
                  COALESCE(SUM(total_amount - COALESCE(gross_received_amount, 0))
                    FILTER (WHERE status IN ('pending', 'partial') AND due_date >= :start AND due_date < :end), 0) AS "totalAReceber",
                  COALESCE(SUM(total_amount - COALESCE(gross_received_amount, 0))
                    FILTER (WHERE status IN ('pending', 'partial') AND due_date >= :start AND due_date < :overdue), 0) AS "aReceberAtrasado",
                  COALESCE(SUM(COALESCE(fee_amount, 0) + COALESCE(discount, 0))
                    FILTER (WHERE status IN ('received', 'partial') AND payment_date >= :start AND payment_date < :end), 0) AS "taxasCartaoMes"
                FROM accounts_receivable
                WHERE (due_date >= :start AND due_date < :end)
                   OR (payment_date >= :start AND payment_date < :end)
                """, params));
        totals.putAll(jdbc.queryForMap("""
                SELECT
                  COALESCE(SUM(total_amount - COALESCE(paid_amount, 0)), 0) AS "totalAPagar",
                  COALESCE(SUM(total_amount - COALESCE(paid_amount, 0))
                    FILTER (WHERE due_date < :overdue), 0) AS "aPagarVencido"
                FROM accounts_payable
                WHERE status IN ('pending', 'partial') AND due_date >= :start AND due_date < :end
                """, params));
        totals.putAll(jdbc.queryForMap("""
                SELECT
                  COALESCE(SUM(CASE WHEN m.type = 'ENTRADA' THEN m.amount ELSE -m.amount END), 0) AS "saldoAtual",
                  COALESCE(SUM(m.amount) FILTER (WHERE m.type = 'ENTRADA' AND m.movement_date >= :start), 0) AS "receitaBrutaMes",
                  COALESCE(SUM(m.amount) FILTER (WHERE m.type = 'SAIDA' AND m.movement_date >= :start), 0) AS "totalDespesasMes",
                  COALESCE(SUM(m.amount) FILTER (WHERE m.type = 'ENTRADA' AND m.movement_date = :today AND m.movement_date >= :start), 0) AS "entradasHoje",
                  COALESCE(SUM(m.amount) FILTER (WHERE m.type = 'ENTRADA' AND m.movement_date >= :start AND r.due_date >= :end), 0) AS "receitasAntecipadas"
                """ + ACTIVE_MOVEMENTS + " AND m.movement_date < :end", params));
        totals.putAll(jdbc.queryForMap("""
                SELECT
                  COUNT(*) FILTER (WHERE LOWER(status) IN ('in_progress', 'pending', 'aberta', 'em_producao')
                    AND created_at >= :start AND created_at < :end) AS "osEmAndamento",
                  COUNT(*) FILTER (WHERE LOWER(status) IN ('completed', 'done', 'delivered', 'concluida')
                    AND install_date >= :start AND install_date < :end) AS "osConcluidasMes",
                  COUNT(*) FILTER (WHERE LOWER(COALESCE(status, '')) NOT IN
                    ('completed', 'done', 'delivered', 'concluida', 'cancelada', 'cancelled', 'canceled')
                    AND install_date < :overdue AND created_at >= :start AND created_at < :end) AS "osAtrasadas"
                FROM work_orders
                WHERE (created_at >= :start AND created_at < :end) OR (install_date >= :start AND install_date < :end)
                """, params));

        totals.putAll(jdbc.queryForMap("""
                SELECT COUNT(*) AS "totalOrcamentos",
                  COUNT(*) FILTER (WHERE LOWER(status) = 'pending') AS "orcamentosPendentes",
                  COUNT(*) FILTER (WHERE LOWER(status) = 'approved') AS "orcamentosAprovados",
                  COALESCE(SUM(total_value) FILTER (WHERE LOWER(status) = 'pending'), 0) AS "orcamentosValor"
                FROM quotes WHERE date_created >= :start AND date_created < :end
                """, params));
        totals.put("ultimasOs", jdbc.queryForList("""
                SELECT w.number, w.title, w.status, w.total_value AS "totalValue",
                       w.deadline_date AS "deadlineDate", w.install_date AS "installDate",
                       COALESCE(c.name, 'Sem cliente') AS "clientName"
                FROM work_orders w LEFT JOIN clients c ON c.id = w.client_id
                WHERE w.created_at >= :start AND w.created_at < :end
                ORDER BY w.created_at DESC, w.id DESC LIMIT 5
                """, params).stream().map(row -> {

                    for (String key : List.of("deadlineDate", "installDate")) {
                        if (row.get(key) instanceof java.sql.Date date) row.put(key, date.toLocalDate());
                    }
                    return row;
                }).toList());
        totals.put("agendaHoje", jdbc.queryForList("""
                SELECT * FROM (
                    SELECT s.id, COALESCE(c.name, 'Sem cliente') AS "clientName",
                           COALESCE(w.title, s.observations, 'Compromisso Agendado') AS title,
                           s.scheduled_time AS "scheduledTime", s.status,
                           'Serviço' AS "entryType", 'wrench' AS icon
                    FROM commercial_schedules s
                    LEFT JOIN clients c ON c.id = s.client_id
                    LEFT JOIN work_orders w ON w.id = s.work_order_id
                    WHERE s.scheduled_date = :today
                    UNION ALL
                    SELECT v.id, COALESCE(c.name, 'Sem cliente') AS "clientName",
                           COALESCE(NULLIF(TRIM(v.notes), ''), 'Visita técnica agendada') AS title,
                           v.visit_time AS "scheduledTime", v.status,
                           'Visita técnica' AS "entryType", 'clipboard-check' AS icon
                    FROM technical_visits v
                    LEFT JOIN clients c ON c.id = v.client_id
                    WHERE v.visit_date = :today
                ) agenda
                ORDER BY "scheduledTime" NULLS LAST, "entryType", id
                """, params).stream().map(row -> {
                    if (row.get("scheduledTime") instanceof java.sql.Time time) {
                        row.put("scheduledTime", time.toLocalTime());
                    }
                    return row;
                }).toList());
        return totals;
    }

    public List<DashboardMonthDto> annualTotals(int year) {
        var params = new MapSqlParameterSource()
                .addValue("start", YearMonth.of(year, 1).atDay(FinancialPeriod.START_DAY))
                .addValue("end", YearMonth.of(year + 1, 1).atDay(FinancialPeriod.START_DAY));
        Map<Integer, BigDecimal> revenue = new HashMap<>();
        jdbc.query("""
                SELECT EXTRACT(MONTH FROM q.date_approved - INTERVAL '5 days') AS month,
                       COALESCE(SUM(w.total_value), 0) AS revenue
                FROM work_orders w JOIN quotes q ON q.id = w.quote_id
                WHERE LOWER(q.status) = 'approved'
                  AND LOWER(COALESCE(w.status, '')) NOT IN ('cancelada', 'cancelled', 'canceled')
                  AND q.date_approved >= :start AND q.date_approved < :end
                GROUP BY 1
                """, params, (RowCallbackHandler) rs ->
                revenue.put(rs.getInt("month"), rs.getBigDecimal("revenue")));
        Map<Integer, BigDecimal> income = new HashMap<>();
        Map<Integer, BigDecimal> expenses = new HashMap<>();
        jdbc.query("""
                SELECT EXTRACT(MONTH FROM m.movement_date - INTERVAL '5 days') AS month,
                  COALESCE(SUM(m.amount) FILTER (WHERE m.type = 'ENTRADA'), 0) AS income,
                  COALESCE(SUM(m.amount) FILTER (WHERE m.type = 'SAIDA'), 0) AS expenses
                """ + ACTIVE_MOVEMENTS + " AND m.movement_date >= :start AND m.movement_date < :end GROUP BY 1",
                params, (RowCallbackHandler) rs -> {
                    income.put(rs.getInt("month"), rs.getBigDecimal("income"));
                    expenses.put(rs.getInt("month"), rs.getBigDecimal("expenses"));
                });
        // Read the recorded closing balance; an absent closing stays null, not zero.
        Map<LocalDate, BigDecimal> closings = new HashMap<>();
        jdbc.query("""
                SELECT period_start, closing_balance FROM financial_closings
                WHERE period_start >= :start AND period_start < :end
                """, params, (RowCallbackHandler) rs ->
                closings.put(rs.getDate("period_start").toLocalDate(), rs.getBigDecimal("closing_balance")));
        var labelFormat = DateTimeFormatter.ofPattern("MMMM", Locale.forLanguageTag("pt-BR"));
        return IntStream.rangeClosed(1, 12).mapToObj(month -> {
            var period = FinancialPeriod.monthly(YearMonth.of(year, month));
            return new DashboardMonthDto(month, period.reference().format(labelFormat), period.from(), period.to(),
                    revenue.getOrDefault(month, BigDecimal.ZERO), income.getOrDefault(month, BigDecimal.ZERO),
                    expenses.getOrDefault(month, BigDecimal.ZERO), closings.get(period.from()));
        }).toList();
    }

    public List<DashboardPaymentMethodDto> paymentMethods(FinancialPeriod period) {
        var params = new MapSqlParameterSource()
                .addValue("start", period.from()).addValue("end", period.endExclusive());

        var counts = jdbc.queryForMap("""
                WITH receipts AS (
                    SELECT r.work_order_id,
                      UPPER(TRANSLATE(TRIM(COALESCE(NULLIF(TRIM(m.payment_method), ''), r.payment_method, '')),
                                      'ãáâàéêíóôõúçÃÁÂÀÉÊÍÓÔÕÚÇ', 'aaaaeeioooucAAAAEEIOOOUC')) AS method
                    FROM financial_movements m
                    JOIN accounts_receivable r ON r.id = m.accounts_receivable_id
                    JOIN work_orders w ON w.id = r.work_order_id
                    WHERE m.type = 'ENTRADA' AND m.amount > 0
                      AND r.status IN ('received', 'partial')
                      AND LOWER(COALESCE(w.status, '')) NOT IN ('cancelada', 'cancelled', 'canceled')
                      AND m.movement_date >= :start AND m.movement_date < :end
                )
                SELECT
                  COUNT(DISTINCT work_order_id) FILTER (WHERE method IN ('CARD', 'CARTAO')) AS card,
                  COUNT(DISTINCT work_order_id) FILTER (WHERE method IN ('CASH', 'DINHEIRO', 'MONEY')) AS cash,
                  COUNT(DISTINCT work_order_id) FILTER (WHERE method = 'PIX') AS pix,
                  COUNT(DISTINCT work_order_id) FILTER (WHERE method = 'BOLETO') AS boleto,
                  COUNT(DISTINCT work_order_id) FILTER (WHERE method IN ('TRANSFER', 'TRANSFERENCIA')) AS transfer
                FROM receipts
                """, params);
        return List.of(
                new DashboardPaymentMethodDto("Cartão", "credit-card", ((Number) counts.get("card")).longValue()),
                new DashboardPaymentMethodDto("Dinheiro", "banknote", ((Number) counts.get("cash")).longValue()),
                new DashboardPaymentMethodDto("Pix", "qr-code", ((Number) counts.get("pix")).longValue()),
                new DashboardPaymentMethodDto("Boleto", "barcode", ((Number) counts.get("boleto")).longValue()),
                new DashboardPaymentMethodDto("Transferência", "arrow-left-right", ((Number) counts.get("transfer")).longValue()));
    }
}
