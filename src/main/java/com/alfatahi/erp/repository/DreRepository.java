package com.alfatahi.erp.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Repository
public class DreRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public DreRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record RevenueRow(UUID id, YearMonth ref, String number, String title, String client,
                             LocalDate date, BigDecimal amount) { }

    public record PayableRow(UUID id, YearMonth ref, String category, String subcategory,
                             boolean financialExpense, String expenseType, String description,
                             String supplier, LocalDate dueDate, LocalDate paymentDate, String status,
                             BigDecimal total, BigDecimal paid, String workOrder) { }

    public record ReceivableRow(UUID id, YearMonth ref, String description, String client, String workOrder,
                                LocalDate paymentDate, BigDecimal discount, BigDecimal fee) { }

    public record LossRow(UUID id, YearMonth ref, String description, String type, String workOrder,
                          LocalDate date, BigDecimal amount) { }

    public List<RevenueRow> revenue(LocalDate start, LocalDate endExclusive) {
        var params = new MapSqlParameterSource().addValue("start", start).addValue("end", endExclusive);
        return jdbc.query("""
                SELECT w.id,
                       CAST(date_trunc('month', COALESCE(q.date_approved, w.created_at) - INTERVAL '5 days') AS date) AS ref,
                       w.number, w.title, COALESCE(c.name, 'Sem cliente') AS client,
                       CAST(COALESCE(q.date_approved, w.created_at) AS date) AS dt,
                       COALESCE(w.total_value, 0) AS amount
                FROM work_orders w
                LEFT JOIN quotes q ON q.id = w.quote_id
                LEFT JOIN clients c ON c.id = w.client_id
                WHERE LOWER(COALESCE(w.status, '')) NOT IN ('cancelada', 'cancelled', 'canceled')
                  AND (w.quote_id IS NULL OR LOWER(q.status) = 'approved')
                  AND COALESCE(q.date_approved, w.created_at) >= :start
                  AND COALESCE(q.date_approved, w.created_at) < :end
                ORDER BY dt, w.number
                """, params, (rs, i) -> new RevenueRow(
                rs.getObject("id", UUID.class), month(rs, "ref"), rs.getString("number"), rs.getString("title"),
                rs.getString("client"), date(rs, "dt"), rs.getBigDecimal("amount")));
    }

    public List<PayableRow> payables(YearMonth first, YearMonth last) {
        var params = new MapSqlParameterSource()
                .addValue("start", first.atDay(1))
                .addValue("end", last.plusMonths(1).atDay(1));
        return jdbc.query("""
                SELECT p.id,
                       CAST(date_trunc('month', CAST(COALESCE(p.competencia, p.due_date - 5) AS timestamp)) AS date) AS ref,
                       p.category, p.subcategory, p.is_financial_expense, p.expense_type, p.description,
                       s.name AS supplier, p.due_date, p.payment_date, p.status,
                       p.total_amount, COALESCE(p.paid_amount, 0) AS paid_amount, w.number AS work_order
                FROM accounts_payable p
                LEFT JOIN suppliers s ON s.id = p.supplier_id
                LEFT JOIN work_orders w ON w.id = p.work_order_id
                WHERE p.status NOT IN ('cancelled', 'inactive')
                  AND COALESCE(p.competencia, p.due_date - 5) >= :start
                  AND COALESCE(p.competencia, p.due_date - 5) < :end
                ORDER BY p.due_date, p.description
                """, params, (rs, i) -> new PayableRow(
                rs.getObject("id", UUID.class), month(rs, "ref"), rs.getString("category"),
                rs.getString("subcategory"), rs.getBoolean("is_financial_expense"), rs.getString("expense_type"),
                rs.getString("description"), rs.getString("supplier"), date(rs, "due_date"),
                date(rs, "payment_date"), rs.getString("status"), rs.getBigDecimal("total_amount"),
                rs.getBigDecimal("paid_amount"), rs.getString("work_order")));
    }

    public List<ReceivableRow> receivableAdjustments(LocalDate start, LocalDate endExclusive) {
        var params = new MapSqlParameterSource().addValue("start", start).addValue("end", endExclusive);
        return jdbc.query("""
                SELECT r.id,
                       CAST(date_trunc('month', r.payment_date - INTERVAL '5 days') AS date) AS ref,
                       r.description, COALESCE(c.name, 'Cliente avulso') AS client, w.number AS work_order,
                       r.payment_date,
                       COALESCE(r.discount, 0) AS discount,
                       CASE WHEN EXISTS (SELECT 1 FROM accounts_payable p
                                          WHERE p.source_receivable_id = r.id
                                            AND p.is_financial_expense
                                            AND p.status NOT IN ('cancelled', 'inactive'))
                            THEN 0 ELSE COALESCE(r.fee_amount, 0) END AS fee
                FROM accounts_receivable r
                LEFT JOIN clients c ON c.id = r.client_id
                LEFT JOIN work_orders w ON w.id = r.work_order_id
                WHERE r.status IN ('received', 'partial')
                  AND r.payment_date >= :start AND r.payment_date < :end
                  AND (COALESCE(r.discount, 0) > 0 OR COALESCE(r.fee_amount, 0) > 0)
                ORDER BY r.payment_date, r.description
                """, params, (rs, i) -> new ReceivableRow(
                rs.getObject("id", UUID.class), month(rs, "ref"), rs.getString("description"),
                rs.getString("client"), rs.getString("work_order"), date(rs, "payment_date"),
                rs.getBigDecimal("discount"), rs.getBigDecimal("fee")));
    }

    public List<LossRow> losses(LocalDate start, LocalDate endExclusive) {
        var params = new MapSqlParameterSource().addValue("start", start).addValue("end", endExclusive);
        return jdbc.query("""
                SELECT l.id,
                       CAST(date_trunc('month', l.occurrence_date - INTERVAL '5 days') AS date) AS ref,
                       l.description, l.type, w.number AS work_order, l.occurrence_date,
                       l.financial_impact
                FROM losses l
                LEFT JOIN work_orders w ON w.id = l.work_order_id
                WHERE l.occurrence_date >= :start AND l.occurrence_date < :end
                ORDER BY l.occurrence_date
                """, params, (rs, i) -> new LossRow(
                rs.getObject("id", UUID.class), month(rs, "ref"), rs.getString("description"),
                rs.getString("type"), rs.getString("work_order"), date(rs, "occurrence_date"),
                rs.getBigDecimal("financial_impact")));
    }

    public int refreshExpenseTypes() {
        return refreshTable("accounts_payable", "p") + refreshTable("expense_recurrences", "r");
    }

    private int refreshTable(String table, String alias) {
        var none = new MapSqlParameterSource();
        int updated = jdbc.update("UPDATE " + table + " " + alias + " "
                + "SET expense_type = CASE WHEN LEFT(s.dre_group, 5) = 'FORA_' THEN 'FORA_DRE' "
                + "ELSE COALESCE(s.cost_behavior, " + alias + ".expense_type) END "
                + "FROM expense_subcategories s JOIN expense_categories cat ON cat.id = s.category_id "
                + "WHERE UPPER(BTRIM(" + alias + ".category)) = UPPER(cat.code) "
                + "AND LOWER(BTRIM(" + alias + ".subcategory)) = LOWER(s.code)", none);

        updated += jdbc.update("UPDATE " + table + " " + alias + " "
                + "SET expense_type = CASE WHEN LEFT(cat.dre_group, 5) = 'FORA_' THEN 'FORA_DRE' ELSE 'VARIAVEL' END "
                + "FROM expense_categories cat "
                + "WHERE UPPER(BTRIM(" + alias + ".category)) = UPPER(cat.code) "
                + "AND (" + alias + ".subcategory IS NULL OR BTRIM(" + alias + ".subcategory) = '') "
                + "AND (LEFT(cat.dre_group, 5) = 'FORA_' OR LEFT(cat.dre_group, 4) IN ('CMV_', 'DED_'))", none);
        return updated;
    }

    private static YearMonth month(ResultSet rs, String column) throws SQLException {
        return YearMonth.from(rs.getDate(column).toLocalDate());
    }

    private static LocalDate date(ResultSet rs, String column) throws SQLException {
        java.sql.Date value = rs.getDate(column);
        return value == null ? null : value.toLocalDate();
    }
}
