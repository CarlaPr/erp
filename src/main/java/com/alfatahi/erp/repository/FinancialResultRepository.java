package com.alfatahi.erp.repository;

import com.alfatahi.erp.dto.FinancialResultFilter;
import com.alfatahi.erp.dto.FinancialResultReport.CashSummary;
import com.alfatahi.erp.dto.FinancialResultReport.Option;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.UUID;

/** Read-only projections: no entity graphs or queries per service/order. */
@Repository
public class FinancialResultRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public FinancialResultRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record OrderRow(UUID id, String number, String description, String client, LocalDate saleDate,
                           BigDecimal sale, String status) { }
    public record Account(UUID id, UUID orderId, boolean receivable, String category, String subcategory,
                          boolean financialExpense, String expenseType, String description, LocalDate dueDate,
                          LocalDate competence, LocalDate paymentDate, String status, BigDecimal total,
                          BigDecimal settled, BigDecimal gross, BigDecimal discount, BigDecimal fee) { }
    public record Allocation(UUID id, UUID accountId, UUID orderId, UUID itemId, BigDecimal value) { }
    public record CostItem(UUID id, UUID orderId, UUID allocationId, String description, BigDecimal amount) { }
    public record CashTotals(UUID accountId, BigDecimal total, BigDecimal before, BigDecimal period,
                             LocalDate first, LocalDate last) { }
    public record MovementRow(UUID id, UUID accountId, boolean receivable, UUID orderId, String orderNumber,
                               LocalDate date, String description, String category, String party, String type,
                               BigDecimal amount, String paymentMethod, BigDecimal accountTotal) { }

    private record BankMatch(UUID movementId, UUID bankId, String status, BigDecimal divergence) { }

    public Map<UUID, String> reconciliations(Collection<UUID> movements) {
        if (movements.isEmpty()) return Map.of();
        Boolean available = jdbc.getJdbcTemplate().execute((ConnectionCallback<Boolean>) connection -> {
            try (ResultSet tables = connection.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
                while (tables.next()) if ("bank_transactions".equalsIgnoreCase(tables.getString("TABLE_NAME"))) return true;
                return false;
            }
        });
        if (!Boolean.TRUE.equals(available)) return Map.of();
        List<BankMatch> matches = jdbc.query("""
                SELECT m.id AS movement_id, b.id AS bank_id, b.status, b.divergence_amount
                FROM financial_movements m JOIN bank_transactions b
                  ON (b.matched_receivable_id=m.accounts_receivable_id OR b.matched_payable_id=m.accounts_payable_id)
                 AND b.transaction_date=m.movement_date AND ABS(b.amount)=ABS(m.amount)
                WHERE m.id IN (:ids)
                """, ids(movements), (rs, i) -> new BankMatch(uuid(rs,"movement_id"), uuid(rs,"bank_id"),
                rs.getString("status"), money(rs,"divergence_amount")));
        Map<UUID, Long> bankCounts = matches.stream().collect(Collectors.groupingBy(BankMatch::bankId, Collectors.counting()));
        Map<UUID, List<BankMatch>> byMovement = matches.stream().collect(Collectors.groupingBy(BankMatch::movementId));
        Map<UUID, String> result = new HashMap<>();
        for (UUID id : movements) {
            List<BankMatch> found = byMovement.getOrDefault(id, List.of());
            String label = found.isEmpty() ? "Sem vínculo bancário comprovado" : found.size() > 1 || bankCounts.get(found.getFirst().bankId()) > 1
                    ? "Vínculo ambíguo; conferir parcela" : found.getFirst().divergence().signum() != 0
                    ? "Vínculo com divergência" : "Vinculado no banco: " + found.getFirst().status();
            result.put(id, label);
        }
        return result;
    }

    private static final String MOVEMENT_FROM = """
            FROM financial_movements m
            LEFT JOIN accounts_receivable r ON r.id = m.accounts_receivable_id
            LEFT JOIN accounts_payable p ON p.id = m.accounts_payable_id
            LEFT JOIN accounts_receivable sr ON sr.id = p.source_receivable_id
            LEFT JOIN work_orders w ON w.id = COALESCE(r.work_order_id, p.work_order_id, sr.work_order_id)
            LEFT JOIN clients c ON c.id = r.client_id
            LEFT JOIN clients wc ON wc.id = w.client_id
            LEFT JOIN suppliers s ON s.id = p.supplier_id
            WHERE ((r.id IS NOT NULL AND r.status IN ('received', 'partial'))
                OR (r.id IS NULL AND p.status IN ('paid', 'partial')))
            """;
    private static final String ORDER_FROM = """
            FROM work_orders w LEFT JOIN quotes q ON q.id = w.quote_id
            LEFT JOIN clients c ON c.id = w.client_id
            WHERE LOWER(COALESCE(w.status, '')) NOT IN ('cancelada', 'cancelled', 'canceled')
              AND (w.quote_id IS NULL OR LOWER(q.status) = 'approved')
            """;
    private static final String SALE_DATE = "CAST(COALESCE(q.date_approved, w.created_at) AS date)";
    private static final String PAYABLE_SELECT = """
            SELECT p.id, COALESCE(p.work_order_id, sr.work_order_id) AS work_order_id,
                   p.category, p.subcategory, p.is_financial_expense, p.expense_type, p.description,
                   p.due_date, p.competencia, p.payment_date, p.status, p.total_amount, p.paid_amount
            FROM accounts_payable p LEFT JOIN accounts_receivable sr ON sr.id = p.source_receivable_id
            WHERE p.status NOT IN ('cancelled', 'inactive', 'canceled')
            """;
    private static final RowMapper<Account> PAYABLE = (rs, i) -> new Account(uuid(rs, "id"), uuid(rs, "work_order_id"),
            false, rs.getString("category"), rs.getString("subcategory"), rs.getBoolean("is_financial_expense"),
            rs.getString("expense_type"), rs.getString("description"), date(rs, "due_date"), date(rs, "competencia"),
            date(rs, "payment_date"), rs.getString("status"), money(rs, "total_amount"), money(rs, "paid_amount"),
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

    public List<OrderRow> orders(FinancialResultFilter filter) {
        String query = "SELECT w.id, w.number, COALESCE(w.description, w.title, '') AS description, "
                + "COALESCE(c.name, 'Sem cliente') AS client, " + SALE_DATE + " AS sale_date, w.total_value, w.status "
                + ORDER_FROM + """
                AND (
                """ + SALE_DATE + " >= :start AND " + SALE_DATE + " < :end OR EXISTS ("
                + "SELECT 1 FROM financial_movements m "
                + "LEFT JOIN accounts_receivable r ON r.id = m.accounts_receivable_id "
                + "LEFT JOIN accounts_payable p ON p.id = m.accounts_payable_id "
                + "LEFT JOIN accounts_receivable sr ON sr.id = p.source_receivable_id "
                + "WHERE m.movement_date >= :start AND m.movement_date < :end "
                + "AND ((r.work_order_id = w.id AND r.status IN ('received','partial')) "
                + "OR (p.status IN ('paid','partial') AND ("
                + "EXISTS (SELECT 1 FROM expense_allocations a WHERE a.accounts_payable_id=p.id AND a.work_order_id=w.id) "
                + "OR (COALESCE(p.work_order_id,sr.work_order_id)=w.id AND NOT EXISTS "
                + "(SELECT 1 FROM expense_allocations a WHERE a.accounts_payable_id=p.id)))))))";
        if (filter.workOrderId() != null) query += " AND w.id=:orderId";
        return jdbc.query(query + " ORDER BY sale_date DESC, w.number", parameters(filter), ORDER);
    }
    private static final RowMapper<OrderRow> ORDER = (rs, i) -> new OrderRow(uuid(rs, "id"), rs.getString("number"),
            rs.getString("description"), rs.getString("client"), date(rs, "sale_date"), money(rs, "total_value"), rs.getString("status"));

    public List<Option> orderOptions() {
        return jdbc.query("SELECT w.id, w.number, COALESCE(c.name, 'Sem cliente') AS client " + ORDER_FROM
                + " ORDER BY w.number DESC", new MapSqlParameterSource(), (rs, i) ->
                new Option(rs.getString("id"), rs.getString("number") + " - " + rs.getString("client")));
    }

    public List<Option> categoryOptions() {
        return jdbc.query("""
                SELECT DISTINCT category AS code FROM accounts_payable WHERE category IS NOT NULL
                UNION SELECT code FROM expense_categories ORDER BY code
                """, new MapSqlParameterSource(), (rs, i) -> new Option(rs.getString("code"), rs.getString("code")));
    }

    public CashSummary cashSummary(FinancialResultFilter filter) {
        String amount = amountExpression(filter);
        String query = "SELECT COALESCE(SUM(CASE WHEN m.movement_date < :start THEN "
                + "CASE WHEN m.type='ENTRADA' THEN " + amount + " ELSE -(" + amount + ") END ELSE 0 END),0) AS opening, "
                + "COALESCE(SUM(CASE WHEN m.movement_date >= :start AND m.type='ENTRADA' THEN " + amount + " ELSE 0 END),0) AS incoming, "
                + "COALESCE(SUM(CASE WHEN m.movement_date >= :start AND m.type='SAIDA' THEN " + amount + " ELSE 0 END),0) AS outgoing, "
                + "COUNT(CASE WHEN m.movement_date >= :start THEN 1 END) AS count "
                + MOVEMENT_FROM + " AND m.movement_date < :end " + movementFilters(filter);
        return jdbc.queryForObject(query, parameters(filter), (rs, i) -> new CashSummary(money(rs, "opening"),
                money(rs, "incoming"), money(rs, "outgoing"), rs.getLong("count")));
    }

    public List<MovementRow> movements(FinancialResultFilter filter) {
        return movements(filter, false);
    }

    public List<MovementRow> history(FinancialResultFilter orderFilter) {
        if (orderFilter.workOrderId() == null) throw new IllegalArgumentException("Informe a OS.");
        return movements(orderFilter, true);
    }

    private List<MovementRow> movements(FinancialResultFilter filter, boolean history) {
        return jdbc.query("""
                SELECT m.id, COALESCE(r.id,p.id) AS account_id,
                       CASE WHEN r.id IS NOT NULL THEN TRUE ELSE FALSE END AS receivable,
                       w.id AS work_order_id, w.number AS order_number, m.movement_date,
                       COALESCE(r.description,p.description,'') AS description,
                       CASE WHEN r.id IS NOT NULL THEN 'RECEITAS' ELSE p.category END AS category,
                       CASE WHEN r.id IS NOT NULL THEN COALESCE(c.name,wc.name,'Cliente avulso')
                            ELSE COALESCE(s.name,'Despesa interna') END AS party, m.type,
                """ + amountExpression(filter) + " AS amount, m.payment_method, COALESCE(p.total_amount,0) AS account_total "
                + MOVEMENT_FROM + (history ? "" : " AND m.movement_date >= :start AND m.movement_date < :end ")
                + movementFilters(filter) + " ORDER BY m.movement_date, m.created_at, m.id", parameters(filter), (rs, i) ->
                new MovementRow(uuid(rs, "id"), uuid(rs, "account_id"), rs.getBoolean("receivable"), uuid(rs, "work_order_id"),
                        rs.getString("order_number"), date(rs, "movement_date"), rs.getString("description"), rs.getString("category"),
                        rs.getString("party"), rs.getString("type"), money(rs, "amount"), rs.getString("payment_method"), money(rs, "account_total")));
    }

    private String movementFilters(FinancialResultFilter f) {
        String sql = "";
        if (!f.category().isEmpty()) sql += " AND ((r.id IS NOT NULL AND :category='RECEITAS') OR (r.id IS NULL AND UPPER(TRIM(p.category))=:category))";
        if (!f.type().isEmpty()) sql += " AND m.type=:type";
        if (!f.receiptStatus().isEmpty()) sql += " AND (r.id IS NULL OR r.status=:receiptStatus)";
        if (!f.paymentStatus().isEmpty()) sql += " AND (p.id IS NULL OR p.status=:paymentStatus)";
        if (f.workOrderId() != null) sql += """
                 AND (r.work_order_id=:orderId OR (r.id IS NULL AND (
                     EXISTS (SELECT 1 FROM expense_allocations a WHERE a.accounts_payable_id=p.id AND a.work_order_id=:orderId)
                     OR (COALESCE(p.work_order_id,sr.work_order_id)=:orderId AND NOT EXISTS
                         (SELECT 1 FROM expense_allocations a WHERE a.accounts_payable_id=p.id)))))
                """;
        return sql;
    }

    private String amountExpression(FinancialResultFilter filter) {
        if (filter.workOrderId() == null) return "m.amount";
        // Allocation takes precedence over a direct order link. The source remains a single ledger row.
        return """
                (CASE WHEN r.id IS NULL AND p.total_amount > 0 AND EXISTS
                    (SELECT 1 FROM expense_allocations a WHERE a.accounts_payable_id=p.id)
                THEN (SELECT COALESCE(SUM(
                """ + allocationMovementAmount() + """
                    ),0) FROM expense_allocations a WHERE a.accounts_payable_id=p.id AND a.work_order_id=:orderId)
                ELSE m.amount END)
                """;
    }

    /** Cumulative rounding assigns residual cents deterministically and conserves every payment. */
    private static String allocationMovementAmount() {
        String cumulative = "SELECT COALESCE(SUM(COALESCE(prior.allocation_value,p.total_amount*prior.percentage/100)),0) "
                + "FROM expense_allocations prior WHERE prior.accounts_payable_id=p.id AND prior.id ";
        return "(ROUND(m.amount*(" + cumulative + "<=a.id)/NULLIF(p.total_amount,0),2) "
                + "-ROUND(m.amount*(" + cumulative + "<a.id)/NULLIF(p.total_amount,0),2))";
    }

    /** accountId identifies the allocation in these projections, so payments can be rounded individually. */
    public List<CashTotals> allocationCash(Collection<UUID> accounts, FinancialResultFilter f) {
        if (accounts.isEmpty()) return List.of();
        String amount = allocationMovementAmount();
        return jdbc.query("SELECT a.id AS allocation_id, COALESCE(SUM(" + amount + "),0) AS total, "
                + "COALESCE(SUM(CASE WHEN m.movement_date<:start THEN " + amount + " ELSE 0 END),0) AS before_amount, "
                + "COALESCE(SUM(CASE WHEN m.movement_date>=:start AND m.movement_date<:end THEN " + amount + " ELSE 0 END),0) AS period_amount, "
                + "MIN(m.movement_date) AS first_date, MAX(m.movement_date) AS last_date "
                + "FROM expense_allocations a JOIN accounts_payable p ON p.id=a.accounts_payable_id "
                + "JOIN financial_movements m ON m.accounts_payable_id=p.id "
                + "WHERE p.id IN (:ids) AND p.status IN ('paid','partial') GROUP BY a.id", parameters(f).addValue("ids", accounts),
                (rs, i) -> new CashTotals(uuid(rs,"allocation_id"), money(rs,"total"), money(rs,"before_amount"),
                        money(rs,"period_amount"), date(rs,"first_date"), date(rs,"last_date")));
    }

    public List<Account> receivables(Collection<UUID> orders) {
        if (orders.isEmpty()) return List.of();
        return jdbc.query("""
                SELECT * FROM accounts_receivable WHERE work_order_id IN (:ids)
                  AND status NOT IN ('cancelled','inactive','canceled')
                """, ids(orders), (rs, i) -> new Account(uuid(rs, "id"), uuid(rs, "work_order_id"), true,
                "RECEITAS", "", false, "", rs.getString("description"), date(rs, "due_date"), null,
                date(rs, "payment_date"), rs.getString("status"), money(rs, "total_amount"), money(rs, "received_amount"),
                money(rs, "gross_received_amount"), money(rs, "discount"), money(rs, "fee_amount")));
    }

    public List<Account> payables(Collection<UUID> orders) {
        if (orders.isEmpty()) return List.of();
        return jdbc.query(PAYABLE_SELECT + " AND (COALESCE(p.work_order_id,sr.work_order_id) IN (:ids) OR EXISTS "
                + "(SELECT 1 FROM expense_allocations a WHERE a.accounts_payable_id=p.id AND a.work_order_id IN (:ids)))", ids(orders), PAYABLE);
    }

    public List<Allocation> allocations(Collection<UUID> accounts) {
        if (accounts.isEmpty()) return List.of();
        return jdbc.query("""
                SELECT a.id, a.accounts_payable_id, a.work_order_id, a.work_order_item_id,
                       COALESCE(a.allocation_value, p.total_amount*a.percentage/100) AS amount
                FROM expense_allocations a JOIN accounts_payable p ON p.id=a.accounts_payable_id
                WHERE a.accounts_payable_id IN (:ids) ORDER BY a.id
                """, ids(accounts), (rs, i) -> new Allocation(uuid(rs, "id"), uuid(rs, "accounts_payable_id"),
                uuid(rs, "work_order_id"), uuid(rs, "work_order_item_id"), money(rs, "amount")));
    }

    public List<CostItem> items(Collection<UUID> orders) {
        if (orders.isEmpty()) return List.of();
        return jdbc.query("""
                SELECT i.id, i.work_order_id, i.source_expense_allocation_id, i.description,
                       COALESCE(i.quantity,0)*COALESCE(i.unit_cost,0) AS amount
                FROM work_order_items i WHERE i.work_order_id IN (:ids)
                """, ids(orders), (rs, i) -> new CostItem(uuid(rs, "id"), uuid(rs, "work_order_id"),
                uuid(rs, "source_expense_allocation_id"), rs.getString("description"), money(rs, "amount")));
    }

    public List<CashTotals> accountCash(Collection<UUID> accounts, FinancialResultFilter f) {
        if (accounts.isEmpty()) return List.of();
        var params = parameters(f).addValue("ids", accounts);
        return jdbc.query("""
                SELECT COALESCE(r.id,p.id) AS account_id, SUM(m.amount) AS total,
                       SUM(CASE WHEN m.movement_date < :start THEN m.amount ELSE 0 END) AS before_amount,
                       SUM(CASE WHEN m.movement_date >= :start AND m.movement_date < :end THEN m.amount ELSE 0 END) AS period_amount,
                       MIN(m.movement_date) AS first_date, MAX(m.movement_date) AS last_date
                """ + MOVEMENT_FROM + " AND (r.id IN (:ids) OR p.id IN (:ids)) GROUP BY COALESCE(r.id,p.id)", params,
                (rs, i) -> new CashTotals(uuid(rs, "account_id"), money(rs, "total"), money(rs, "before_amount"),
                        money(rs, "period_amount"), date(rs, "first_date"), date(rs, "last_date")));
    }

    public List<Account> competencePayables(FinancialResultFilter f) {
        var params = parameters(f).addValue("monthStart", f.from().withDayOfMonth(1))
                .addValue("monthEnd", f.to().withDayOfMonth(1).plusMonths(1));
        return jdbc.query(PAYABLE_SELECT + " AND ((p.competencia IS NOT NULL AND p.competencia>=:monthStart AND p.competencia<:monthEnd) "
                + "OR (p.competencia IS NULL AND p.due_date>=:start AND p.due_date<:end))", params, PAYABLE);
    }

    public BigDecimal competenceRevenue(FinancialResultFilter f) {
        return jdbc.queryForObject("SELECT COALESCE(SUM(w.total_value),0) " + ORDER_FROM + " AND "
                + SALE_DATE + ">=:start AND " + SALE_DATE + "<:end", parameters(f), BigDecimal.class);
    }

    public boolean hasUndatedCostEstimates(FinancialResultFilter f) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM work_order_items i JOIN work_orders w ON w.id=i.work_order_id "
                + "LEFT JOIN quotes q ON q.id=w.quote_id WHERE " + SALE_DATE + ">=:start AND " + SALE_DATE + "<:end "
                + "AND LOWER(COALESCE(w.status,'')) NOT IN ('cancelada','cancelled','canceled') "
                + "AND (w.quote_id IS NULL OR LOWER(q.status)='approved') "
                + "AND i.source_expense_allocation_id IS NULL AND COALESCE(i.quantity,0)*COALESCE(i.unit_cost,0)<>0 "
                + "AND NOT EXISTS (SELECT 1 FROM expense_allocations a WHERE a.work_order_item_id=i.id) "
                + "AND NOT (i.description LIKE '[OP] %||| Taxa de Maquininha%' OR i.description LIKE '[OP] %||| Desconto Concedido%')",
                parameters(f), Long.class);
        return count != null && count > 0;
    }

    public List<Account> adjustments(FinancialResultFilter f) {
        return jdbc.query("""
                SELECT r.id, r.work_order_id, r.payment_date, r.discount,
                  CASE WHEN EXISTS (SELECT 1 FROM accounts_payable p WHERE p.source_receivable_id=r.id
                    AND p.is_financial_expense=TRUE AND p.status NOT IN ('cancelled','inactive','canceled'))
                    THEN 0 ELSE COALESCE(r.fee_amount,0) END AS fee
                FROM accounts_receivable r WHERE r.status IN ('received','partial')
                  AND r.payment_date>=:start AND r.payment_date<:end
                  AND (COALESCE(r.discount,0)>0 OR COALESCE(r.fee_amount,0)>0)
                """, parameters(f), (rs, i) -> new Account(uuid(rs, "id"), uuid(rs, "work_order_id"), true, "RECEITAS", "",
                false, "", "", null, null, date(rs, "payment_date"), "", BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, money(rs, "discount"), money(rs, "fee")));
    }

    private static MapSqlParameterSource parameters(FinancialResultFilter f) {
        return new MapSqlParameterSource().addValue("start", f.from()).addValue("end", f.endExclusive())
                .addValue("category", f.category()).addValue("orderId", f.workOrderId()).addValue("type", f.type())
                .addValue("receiptStatus", f.receiptStatus()).addValue("paymentStatus", f.paymentStatus());
    }
    private static MapSqlParameterSource ids(Collection<UUID> ids) { return new MapSqlParameterSource("ids", ids); }
    private static UUID uuid(ResultSet rs, String column) throws SQLException { return rs.getObject(column, UUID.class); }
    private static LocalDate date(ResultSet rs, String column) throws SQLException {
        java.sql.Date value = rs.getDate(column);
        return value == null ? null : value.toLocalDate();
    }
    private static BigDecimal money(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return value == null ? BigDecimal.ZERO : value;
    }
}
