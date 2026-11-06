package com.alfatahi.erp.service;

import com.alfatahi.erp.dto.FinancialResultFilter;
import com.alfatahi.erp.dto.FinancialResultReport;
import com.alfatahi.erp.dto.FinancialResultReport.*;
import com.alfatahi.erp.repository.FinancialResultRepository;
import com.alfatahi.erp.repository.FinancialResultRepository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class FinancialResultService {
    public static class InvalidAllocationException extends IllegalArgumentException {
        public InvalidAllocationException() { super("Há rateios inválidos nesta OS. Corrija os valores em Contas a Pagar antes de consultar seu caixa individual. O caixa geral permanece disponível sem o filtro de OS."); }
    }
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private final FinancialResultRepository repository;
    private final DreService dreService;

    public FinancialResultService(FinancialResultRepository repository, DreService dreService) {
        this.repository = repository;
        this.dreService = dreService;
    }

    public List<Option> orderOptions() { return repository.orderOptions(); }
    public List<Option> categoryOptions() {
        List<Option> options = new ArrayList<>();
        options.add(new Option("RECEITAS", "Receitas / recebimentos"));
        repository.categoryOptions().stream().filter(o -> !"RECEITAS".equalsIgnoreCase(o.value())).forEach(options::add);
        return options;
    }

    public FinancialResultReport report(FinancialResultFilter filter) {
        DreService.Catalog catalog = dreService.loadCatalog();
        List<OrderRow> candidates = repository.orders(filter);
        List<UUID> orderIds = new ArrayList<>(candidates.stream().map(OrderRow::id).toList());
        // A selected order may only have an opening balance and no activity in this period.
        if (filter.workOrderId() != null && !orderIds.contains(filter.workOrderId())) orderIds.add(filter.workOrderId());
        List<Account> receivables = repository.receivables(orderIds);
        List<Account> payables = repository.payables(orderIds);
        List<MovementRow> ledger = repository.movements(filter);
        Set<UUID> payableIds = payables.stream().map(Account::id).collect(Collectors.toCollection(LinkedHashSet::new));
        ledger.stream().filter(m -> !m.receivable()).map(MovementRow::accountId).forEach(payableIds::add);
        List<Allocation> allocations = repository.allocations(payableIds);
        Map<UUID, List<Allocation>> byAccount = allocations.stream().collect(Collectors.groupingBy(Allocation::accountId));
        Map<UUID, OrderRow> orderMap = candidates.stream().collect(Collectors.toMap(OrderRow::id, Function.identity()));
        Map<UUID, String> reconciliations = repository.reconciliations(ledger.stream().map(MovementRow::id).toList());
        List<Movement> movements = ledger.stream().map(m -> movement(m, byAccount, orderMap, reconciliations)).sorted(movementComparator(filter.sort())).toList();

        Set<UUID> allAccounts = new LinkedHashSet<>(payableIds);
        receivables.stream().map(Account::id).forEach(allAccounts::add);
        Map<UUID, CashTotals> cash = repository.accountCash(allAccounts, filter).stream()
                .collect(Collectors.toMap(CashTotals::accountId, Function.identity()));
        Map<UUID, CashTotals> allocatedCash = repository.allocationCash(payableIds, filter).stream()
                .collect(Collectors.toMap(CashTotals::accountId, Function.identity()));
        Map<UUID, List<Account>> receiptsByOrder = receivables.stream().collect(Collectors.groupingBy(Account::orderId));
        Map<UUID, List<CostItem>> items = repository.items(orderIds).stream().collect(Collectors.groupingBy(CostItem::orderId));
        Set<UUID> linkedItems = allocations.stream().map(Allocation::itemId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, List<AccountShare>> expensesByOrder = new HashMap<>();
        for (Account account : payables) {
            List<Allocation> shares = byAccount.getOrDefault(account.id(), List.of());
            if (shares.isEmpty()) {
                if (account.orderId() != null) expensesByOrder.computeIfAbsent(account.orderId(), k -> new ArrayList<>())
                        .add(new AccountShare(account, null, account.total(), account.total().subtract(account.settled()).max(ZERO), false));
            } else {
                BigDecimal allocated = shares.stream().map(Allocation::value).reduce(ZERO, BigDecimal::add);
                boolean invalid = account.total().signum() <= 0 || allocated.compareTo(account.total()) > 0
                        || shares.stream().anyMatch(a -> a.value().signum() < 0);
                BigDecimal cumulative = ZERO;
                for (Allocation share : shares) {
                    BigDecimal pending = allocatedAmount(account.total().subtract(account.settled()).max(ZERO), cumulative, share.value(), account.total());
                    expensesByOrder.computeIfAbsent(share.orderId(), k -> new ArrayList<>()).add(new AccountShare(account, share.id(), share.value(), pending, invalid));
                    cumulative = cumulative.add(share.value());
                }
            }
        }
        List<OrderResult> results = new ArrayList<>();
        if (filter.workOrderId() != null && expensesByOrder.getOrDefault(filter.workOrderId(), List.of()).stream().anyMatch(AccountShare::invalid)) {
            throw new InvalidAllocationException();
        }
        for (OrderRow order : candidates) {
            List<Account> receipts = receiptsByOrder.getOrDefault(order.id(), List.of());
            List<AccountShare> expenses = expensesByOrder.getOrDefault(order.id(), List.of());
            if (!matches(filter, receipts, expenses)) continue;
            results.add(orderResult(order, filter, receipts, expenses, cash, allocatedCash, catalog,
                    items.getOrDefault(order.id(), List.of()), linkedItems));
        }
        BigDecimal oldReceipts = ZERO, newReceivables = ZERO, oldPayments = ZERO, pendingCosts = ZERO;
        for (OrderResult order : results) {
            if (order.saleDate() != null && order.saleDate().isBefore(filter.from())) {
                oldReceipts = oldReceipts.add(order.receivedPeriod());
                oldPayments = oldPayments.add(order.directPaidPeriod());
            }
            if (order.soldInPeriod()) newReceivables = newReceivables.add(order.receivable());
            if (!completed(order.status())) pendingCosts = pendingCosts.add(order.payable());
        }
        Set<String> warnings = new LinkedHashSet<>();
        warnings.add("Conciliação utiliza vínculos bancários legados por título, data e valor, sem somar transações bancárias ao caixa. Ausência ou ambiguidade de vínculo não comprova conciliação. A data efetiva de conclusão não é registrada e não é inferida do agendamento.");
        warnings.add("Histórico anterior à implantação dos movimentos individuais pode ter sido consolidado na última data de pagamento (migração V29). Não é possível reconstruir parcelas antigas sem seus registros originais.");
        warnings.add("Recebimentos de caixa são líquidos das taxas de cartão, conforme o fluxo de caixa existente. Pendências usam as baixas brutas e os descontos registrados nas contas.");
        List<ResultLine> operating = operating(filter, catalog, warnings);
        return new FinancialResultReport(filter, repository.cashSummary(filter), repository.cashSummary(filter.previous()),
                results, movements, new CrossPeriod(results.stream().filter(OrderResult::spansMonths).toList(),
                oldReceipts, newReceivables, oldPayments, pendingCosts), operating, List.copyOf(warnings));
    }

    public List<Movement> history(UUID id) {
        LocalDate today = FinancialPeriod.today();
        var filter = new FinancialResultFilter(today, today, "", id, "", "", "", "date");
        List<MovementRow> rows = repository.history(filter);
        List<UUID> ids = rows.stream().filter(m -> !m.receivable()).map(MovementRow::accountId).distinct().toList();
        Map<UUID, List<Allocation>> allocations = repository.allocations(ids).stream().collect(Collectors.groupingBy(Allocation::accountId));
        for (MovementRow row : rows) {
            List<Allocation> shares = allocations.getOrDefault(row.accountId(), List.of());
            if (!shares.isEmpty() && (row.accountTotal().signum() <= 0 || shares.stream().map(Allocation::value).reduce(ZERO, BigDecimal::add).compareTo(row.accountTotal()) > 0
                    || shares.stream().anyMatch(a -> a.value().signum() < 0))) throw new InvalidAllocationException();
        }
        Map<UUID, String> reconciliations = repository.reconciliations(rows.stream().map(MovementRow::id).toList());
        return rows.stream().map(m -> movement(m, allocations, Map.of(), reconciliations)).toList();
    }

    private record AccountShare(Account account, UUID allocationId, BigDecimal cost, BigDecimal pending, boolean invalid) { }

    private boolean matches(FinancialResultFilter f, List<Account> receipts, List<AccountShare> expenses) {
        if (!f.receiptStatus().isEmpty() && receipts.stream().noneMatch(a -> a.status().equals(f.receiptStatus()))) return false;
        if (!f.paymentStatus().isEmpty() && expenses.stream().noneMatch(a -> a.account().status().equals(f.paymentStatus()))) return false;
        if (f.category().isEmpty()) return true;
        return "RECEITAS".equals(f.category()) ? !receipts.isEmpty()
                : expenses.stream().anyMatch(a -> f.category().equalsIgnoreCase(a.account().category()));
    }

    private OrderResult orderResult(OrderRow order, FinancialResultFilter f, List<Account> receipts,
                                    List<AccountShare> expenses, Map<UUID, CashTotals> cash, Map<UUID, CashTotals> allocatedCash, DreService.Catalog catalog,
                                    List<CostItem> items, Set<UUID> linkedItems) {
        Stats incoming = new Stats(), outgoing = new Stats(), directOutgoing = new Stats();
        List<String> warnings = new ArrayList<>();
        BigDecimal pendingReceipt = ZERO, pendingPayment = ZERO;
        BigDecimal receiptTitles = ZERO;
        for (Account account : receipts) {
            incoming.add(cash.get(account.id()), BigDecimal.ONE);
            BigDecimal gross = account.gross().signum() != 0 ? account.gross() : account.settled().add(account.fee());
            pendingReceipt = pendingReceipt.add(account.total().subtract(gross).subtract(account.discount()).max(ZERO));
            receiptTitles = receiptTitles.add(account.total());
            checkHistory(account, cash.get(account.id()), warnings);
        }
        if (receipts.isEmpty()) {
            pendingReceipt = order.sale().max(ZERO);
            warnings.add("Venda sem títulos a receber: a pendência corresponde ao valor vendido. Cadastre/vincule as contas para confirmar o saldo.");
        } else if (receiptTitles.compareTo(order.sale()) != 0) warnings.add("O total dos títulos a receber difere do valor vendido. Revise parcelas e vínculos.");
        BigDecimal[] linked = {ZERO, ZERO, ZERO}, manual = {ZERO, ZERO, ZERO};
        boolean[] linkedClass = new boolean[3];
        boolean reliable = true;
        List<CostDetail> costDetails = new ArrayList<>();
        for (AccountShare share : expenses) {
            Account account = share.account();
            CashTotals accountCash = share.allocationId() == null ? cash.get(account.id()) : allocatedCash.get(share.allocationId());
            outgoing.add(accountCash, BigDecimal.ONE);
            checkHistory(account, cash.get(account.id()), warnings);
            DreService.Resolved resolved = catalog.resolve(account.category(), account.subcategory(), account.financialExpense(), account.expenseType());
            DreGroup group = resolved.group();
            boolean direct = directCost(group);
            costDetails.add(new CostDetail(account.description(), resolved.label(), account.dueDate(), account.paymentDate(),
                    account.status(), share.cost(), accountCash == null ? ZERO : accountCash.total(), share.pending(),
                    direct, false, share.invalid()));
            if (directCost(group)) {
                directOutgoing.add(accountCash, BigDecimal.ONE);
                int kind = costKind(group);
                linked[kind] = linked[kind].add(share.cost());
                linkedClass[kind] = true;
                pendingPayment = pendingPayment.add(share.pending());
                if (group == DreGroup.NAO_CLASSIFICADO) {
                    warnings.add("Há custo vinculado sem classificação na DRE; confirme se é custo direto.");
                    reliable = false;
                }
            }
            if (share.invalid()) {
                warnings.add("Rateio inválido: valores excedem o total da conta ou a conta possui valor inválido. Revise antes de avaliar a rentabilidade.");
                reliable = false;
            }
        }
        for (CostItem item : items) {
            if (item.allocationId() != null || linkedItems.contains(item.id()) || item.amount().signum() == 0) continue;
            String description = item.description() == null ? "" : item.description();
            if (description.startsWith("[OP] ") && (description.contains("||| Taxa de Maquininha") || description.contains("||| Desconto Concedido"))) continue;
            int kind = !description.startsWith("[OP] ") ? 0
                    : description.startsWith("[OP] Mão de Obra") || description.startsWith("[OP] Instalação") ? 1 : 2;
            manual[kind] = manual[kind].add(item.amount());
            costDetails.add(new CostDetail(manualDescription(description), switch (kind) {
                case 0 -> "Materiais e insumos (estimativa manual)";
                case 1 -> "Mão de obra / instalação (estimativa manual)";
                default -> "Outros custos diretos (estimativa manual)";
            }, null, null, "Estimativa manual", item.amount(), ZERO, ZERO, !linkedClass[kind], true, false));
        }
        BigDecimal[] costs = new BigDecimal[3];
        for (int i = 0; i < 3; i++) {
            costs[i] = linkedClass[i] ? linked[i] : manual[i];
            if (linkedClass[i] && manual[i].signum() != 0) {
                warnings.add("Custos manuais e contas vinculadas na mesma classe sem rastreabilidade: estimativas manuais não foram somadas. Vincule os custos para eliminar possível duplicidade.");
                reliable = false;
            }
        }
        BigDecimal totalCosts = Arrays.stream(costs).reduce(ZERO, BigDecimal::add);
        if (totalCosts.signum() == 0) {
            warnings.add("Nenhum custo direto cadastrado; lucro e margem não calculados.");
            reliable = false;
        }
        BigDecimal profit = reliable ? order.sale().subtract(totalCosts) : null;
        BigDecimal margin = profit != null && order.sale().signum() > 0
                ? profit.multiply(BigDecimal.valueOf(100)).divide(order.sale(), 2, RoundingMode.HALF_UP) : null;
        boolean sold = f.contains(order.saleDate());
        List<String> situations = new ArrayList<>();
        if (sold && incoming.total.signum() > 0 && pendingReceipt.signum() > 0) situations.add("Venda do período com recebimento parcial");
        if (order.saleDate() != null && order.saleDate().isBefore(f.from()) && incoming.period.signum() > 0) situations.add("Recebimento de venda anterior");
        if (outgoing.period.signum() > 0 && pendingReceipt.signum() > 0) situations.add("Custos pagos com recebimentos pendentes");
        if (incoming.total.signum() > 0 && pendingPayment.signum() > 0) situations.add("Recebido com custos pendentes");
        boolean manualCostsWithoutPaymentEvidence = Arrays.stream(manual).anyMatch(amount -> amount.signum() != 0);
        if (!receipts.isEmpty() && !expenses.isEmpty() && !manualCostsWithoutPaymentEvidence && reliable
                && pendingReceipt.signum() == 0 && pendingPayment.signum() == 0) situations.add("Recebimentos e contas vinculadas quitados");
        LocalDate first = min(incoming.first, outgoing.first), last = max(incoming.last, outgoing.last);
        boolean spans = first != null && order.saleDate() != null && (!YearMonth.from(first).equals(YearMonth.from(order.saleDate()))
                || !YearMonth.from(last).equals(YearMonth.from(order.saleDate()))) || pendingReceipt.signum() > 0 || pendingPayment.signum() > 0;
        if (last != null && last.isAfter(FinancialPeriod.today())) warnings.add("Há movimentos com data futura registrados nesta OS. Confira as datas de liquidação.");
        return new OrderResult(order.id(), order.number(), order.client(), order.description(), order.saleDate(), order.status(),
                order.sale(), incoming.total, incoming.period, incoming.before, pendingReceipt,
                costs[0], costs[1], costs[2], totalCosts, outgoing.total, outgoing.period, outgoing.before, pendingPayment,
                profit, margin, incoming.period.subtract(outgoing.period), last, directOutgoing.period, sold, spans, situations,
                warnings.stream().distinct().toList(), costDetails.stream()
                        .sorted(Comparator.comparing(CostDetail::category, String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(CostDetail::dueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(CostDetail::description, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                        .toList());
    }

    private static String manualDescription(String description) {
        if (description.startsWith("[OP] ")) description = description.substring(5);
        int separator = description.indexOf(" ||| ");
        return separator < 0 ? description : description.substring(0, separator);
    }

    private void checkHistory(Account account, CashTotals cash, List<String> warnings) {
        if (account.settled().compareTo(cash == null ? ZERO : cash.total()) != 0) {
            warnings.add("Valores liquidados no título diferem do histórico individual de caixa. O caixa usa somente os movimentos existentes; revise o lançamento original.");
        }
    }

    private Movement movement(MovementRow row, Map<UUID, List<Allocation>> allocations, Map<UUID, OrderRow> orders, Map<UUID, String> reconciliations) {
        List<Allocation> shares = row.receivable() ? List.of() : allocations.getOrDefault(row.accountId(), List.of());
        List<Option> links;
        if (!shares.isEmpty()) {
            links = shares.stream().map(a -> new Option(a.orderId().toString(), orders.containsKey(a.orderId())
                    ? orders.get(a.orderId()).number() : "OS vinculada")).distinct().toList();
        } else links = row.orderId() == null ? List.of() : List.of(new Option(row.orderId().toString(), row.orderNumber()));
        boolean incoming = "ENTRADA".equals(row.type());
        String url = (row.receivable() ? "/receivables" : "/payables") + "?allMonths=true&entryId=" + row.accountId();
        return new Movement(row.id(), row.date(), row.description(), row.category(), row.party(), row.type(),
                incoming ? row.amount() : ZERO, incoming ? ZERO : row.amount(), row.paymentMethod(), url, links, !shares.isEmpty(),
                reconciliations.getOrDefault(row.id(), "Não disponível"));
    }

    private List<ResultLine> operating(FinancialResultFilter f, DreService.Catalog catalog, Set<String> warnings) {
        Map<DreGroup, BigDecimal> amounts = new EnumMap<>(DreGroup.class);
        for (Account account : repository.competencePayables(f)) {
            LocalDate date = account.competence() == null ? account.dueDate() : YearMonth.from(account.competence()).atDay(FinancialPeriod.START_DAY);
            if (!f.contains(date)) continue;
            DreGroup group = catalog.resolve(account.category(), account.subcategory(), account.financialExpense(), account.expenseType()).group();
            amounts.merge(group, account.total(), BigDecimal::add);
        }
        BigDecimal discounts = ZERO;
        for (Account adjustment : repository.adjustments(f)) {
            discounts = discounts.add(adjustment.discount());
            amounts.merge(DreGroup.DESP_FIN_CARTAO, adjustment.fee(), BigDecimal::add);
        }
        BigDecimal revenue = repository.competenceRevenue(f);
        BigDecimal taxes = value(amounts, DreGroup.DED_IMPOSTOS_VENDAS);
        BigDecimal costs = sumSection(amounts, DreGroup.Section.CUSTOS);
        BigDecimal admin = value(amounts, DreGroup.DESP_ADMIN);
        BigDecimal generalTaxes = value(amounts, DreGroup.DESP_TRIBUTARIAS);
        BigDecimal operating = sumSection(amounts, DreGroup.Section.OPERACIONAIS).subtract(admin).subtract(generalTaxes);
        BigDecimal financial = sumSection(amounts, DreGroup.Section.FINANCEIRO);
        BigDecimal incomeTax = value(amounts, DreGroup.IR_CSLL);
        BigDecimal unclassified = value(amounts, DreGroup.NAO_CLASSIFICADO);
        BigDecimal gross = revenue.subtract(discounts).subtract(taxes).subtract(costs);
        BigDecimal net = gross.subtract(operating).subtract(admin).subtract(generalTaxes).subtract(financial).subtract(incomeTax).subtract(unclassified);
        boolean undatedEstimates = repository.hasUndatedCostEstimates(f);
        if (undatedEstimates) warnings.add("OS vendidas no período possuem custos manuais sem vínculo financeiro e sem competência comprovável. O resultado mensal é provisório; vincule os títulos para apurá-lo integralmente.");
        if (unclassified.signum() != 0) warnings.add("Há despesas sem classificação: o resultado por competência é provisório e inclui esses valores. Revise o plano de contas da DRE.");
        warnings.add("Resultado por competência considera somente o período, para a empresa inteira. Custos manuais sem título financeiro não têm competência comprovável e não são adicionados à DRE. Taxas/descontos acumulados seguem a última data do recebimento, como na DRE atual.");
        return List.of(new ResultLine("Receita dos serviços vendidos no período", revenue, false, false),
                new ResultLine("(-) Descontos concedidos", discounts.negate(), false, false),
                new ResultLine("(-) Impostos sobre vendas", taxes.negate(), false, false),
                new ResultLine("(-) Custos diretos por competência", costs.negate(), false, false),
                new ResultLine("(=) Lucro bruto por competência", gross, true, false),
                new ResultLine("(-) Despesas operacionais", operating.negate(), false, false),
                new ResultLine("(-) Despesas administrativas", admin.negate(), false, false),
                new ResultLine("(-) Impostos e taxas gerais", generalTaxes.negate(), false, false),
                new ResultLine("(-) Despesas financeiras (juros, encargos e cartão)", financial.negate(), false, false),
                new ResultLine("(-) IRPJ / CSLL", incomeTax.negate(), false, false),
                new ResultLine("(-) Não classificado (revisar)", unclassified.negate(), false, false),
                new ResultLine("(=) Resultado líquido por competência" + (unclassified.signum() != 0 || undatedEstimates ? " (provisório: revisar custos/classificação)" : ""), net, true, false),
                new ResultLine("Informativo: principal de financiamentos, investimentos, reservas e retiradas", sumSection(amounts, DreGroup.Section.FORA_DRE), false, true));
    }

    private static boolean directCost(DreGroup group) {
        return group.getSection() == DreGroup.Section.CUSTOS || group.getSection() == DreGroup.Section.OPERACIONAIS || group == DreGroup.NAO_CLASSIFICADO;
    }
    private static int costKind(DreGroup group) { return group == DreGroup.CMV_MATERIAIS ? 0 : group == DreGroup.CMV_INSTALACAO ? 1 : 2; }
    private static BigDecimal value(Map<DreGroup, BigDecimal> amounts, DreGroup group) { return amounts.getOrDefault(group, ZERO); }
    private static BigDecimal allocatedAmount(BigDecimal amount, BigDecimal prior, BigDecimal share, BigDecimal total) {
        if (total.signum() == 0) return ZERO;
        return amount.multiply(prior.add(share)).divide(total, 2, RoundingMode.HALF_UP)
                .subtract(amount.multiply(prior).divide(total, 2, RoundingMode.HALF_UP));
    }
    private static BigDecimal sumSection(Map<DreGroup, BigDecimal> amounts, DreGroup.Section section) {
        return amounts.entrySet().stream().filter(e -> e.getKey().getSection() == section).map(Map.Entry::getValue).reduce(ZERO, BigDecimal::add);
    }
    private static boolean completed(String status) { return Set.of("completed", "done", "delivered", "concluida").contains(status == null ? "" : status.toLowerCase(Locale.ROOT)); }
    private static LocalDate min(LocalDate a, LocalDate b) { return a == null ? b : b == null || a.isBefore(b) ? a : b; }
    private static LocalDate max(LocalDate a, LocalDate b) { return a == null ? b : b == null || a.isAfter(b) ? a : b; }
    private static Comparator<Movement> movementComparator(String sort) {
        Comparator<Movement> comparator = switch (sort) {
            case "date_desc" -> Comparator.comparing(Movement::date).reversed();
            case "amount_desc" -> Comparator.comparing(Movement::amount).reversed();
            case "description" -> Comparator.comparing(Movement::description, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(Movement::date);
        };
        return comparator.thenComparing(Movement::id);
    }
    private static final class Stats {
        BigDecimal total = ZERO, before = ZERO, period = ZERO;
        LocalDate first, last;
        void add(CashTotals cash, BigDecimal ratio) {
            if (cash == null) return;
            total = total.add(cash.total().multiply(ratio));
            before = before.add(cash.before().multiply(ratio));
            period = period.add(cash.period().multiply(ratio));
            first = min(first, cash.first());
            last = max(last, cash.last());
        }
    }
}
