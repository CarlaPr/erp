package com.alfatahi.erp.service;

import com.alfatahi.erp.entity.AccountsReceivable;
import com.alfatahi.erp.entity.Client;
import com.alfatahi.erp.entity.Profile;
import com.alfatahi.erp.entity.WorkOrder;
import com.alfatahi.erp.entity.WorkOrderItem;
import com.alfatahi.erp.repository.AccountsReceivableRepository;
import com.alfatahi.erp.repository.ProfileRepository;
import com.alfatahi.erp.util.WorkOrderStatus;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WorkOrderReportPdfService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final Set<String> INACTIVE_ACCOUNTS = Set.of("cancelled", "canceled", "inactive");

    public record Amounts(BigDecimal contracted, BigDecimal cost, BigDecimal gross, BigDecimal net, BigDecimal balance) {
        Amounts add(Amounts other) {
            return new Amounts(contracted.add(other.contracted), cost.add(other.cost), gross.add(other.gross),
                    net.add(other.net), balance.add(other.balance));
        }

        public BigDecimal profit() { return contracted.subtract(cost); }

        public BigDecimal margin() {
            return contracted.signum() <= 0 ? ZERO : profit().movePointRight(2).divide(contracted, 2, RoundingMode.HALF_UP);
        }
    }

    public record Payment(AccountsReceivable account, BigDecimal gross, BigDecimal net, BigDecimal balance) {
        static Payment of(AccountsReceivable account) {
            BigDecimal gross = account.getGrossReceivedAmount();
            BigDecimal net = account.getNetReceivedAmount();
            BigDecimal fee = account.getFeeAmount();
            if (gross.signum() == 0) gross = net.add(fee);
            // Baixas antigas podem ter apenas o status de quitação, sem o valor lançado.
            if (gross.signum() == 0 && "received".equalsIgnoreCase(account.getStatus())) {
                gross = account.getTotalAmount();
            }
            if (net.signum() == 0) net = gross.subtract(fee).max(ZERO);
            return new Payment(account, gross, net, account.getTotalAmount().subtract(gross).max(ZERO));
        }
    }

    public record Item(WorkOrderItem item, String description, String detail) {
        static Item of(WorkOrderItem item) {
            String description = item.getDescription();
            if (description == null || !description.startsWith("[OP] ")) return new Item(item, description, "");
            String[] parts = description.substring(5).split(" \\|\\|\\| ", 2);
            String details = parts[0].replaceFirst("\\s*\\|\\s*$", "");
            return parts.length > 1 && !parts[1].isBlank()
                    ? new Item(item, parts[1], details) : new Item(item, details, "");
        }
    }

    public record Order(WorkOrder workOrder, Client client, String status, List<Item> items,
                        List<Payment> payments, Amounts amounts) { }

    private final TemplateEngine templateEngine;
    private final ProfileRepository profileRepository;
    private final CompanyImageService companyImageService;
    private final AccountsReceivableRepository receivableRepository;

    public WorkOrderReportPdfService(TemplateEngine templateEngine, ProfileRepository profileRepository,
                                     CompanyImageService companyImageService, AccountsReceivableRepository receivableRepository) {
        this.templateEngine = templateEngine;
        this.profileRepository = profileRepository;
        this.companyImageService = companyImageService;
        this.receivableRepository = receivableRepository;
    }

    public byte[] render(List<WorkOrder> orders, FinancialPeriod period, String status, String search,
                         boolean unscheduled) throws Exception {
        Map<UUID, List<AccountsReceivable>> accountsByOrder = orders.isEmpty() ? Map.of()
                : receivableRepository.findByWorkOrderIdIn(orders.stream().map(WorkOrder::getId).toList()).stream()
                        .filter(account -> !INACTIVE_ACCOUNTS.contains(account.getStatus().toLowerCase(Locale.ROOT)))
                        .collect(Collectors.groupingBy(account -> account.getWorkOrder().getId()));

        List<Order> details = orders.stream()
                .map(order -> detail(order, accountsByOrder.getOrDefault(order.getId(), List.of())))
                .toList();
        Amounts totals = details.stream().filter(order -> !WorkOrderStatus.isCancelled(order.workOrder()))
                .map(Order::amounts).reduce(new Amounts(ZERO, ZERO, ZERO, ZERO, ZERO), Amounts::add);

        Profile company = companyProfile();
        Context context = new Context(Locale.forLanguageTag("pt-BR"));
        context.setVariable("company", company);
        context.setVariable("logoBase64", companyImageService.logoDataUri(company));
        context.setVariable("period", period);
        context.setVariable("generatedDate", FinancialPeriod.today());
        context.setVariable("statusFilter", status);
        context.setVariable("search", search);
        context.setVariable("unscheduled", unscheduled);
        context.setVariable("orders", details);
        context.setVariable("totals", totals);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.withHtmlContent(templateEngine.process("work-order-report-pdf", context), null);
        builder.toStream(output);
        builder.run();
        return output.toByteArray();
    }

    private Order detail(WorkOrder order, List<AccountsReceivable> accounts) {
        List<Payment> payments = accounts.stream()
                .sorted(Comparator.comparing(AccountsReceivable::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(Payment::of).toList();
        BigDecimal gross = ZERO;
        BigDecimal net = ZERO;
        BigDecimal balance = ZERO;
        for (Payment payment : payments) {
            gross = gross.add(payment.gross());
            net = net.add(payment.net());
            balance = balance.add(payment.balance());
        }
        boolean cancelled = WorkOrderStatus.isCancelled(order);
        if (payments.isEmpty() && !cancelled) balance = order.getTotalValue().max(ZERO);
        Amounts amounts = new Amounts(order.getTotalValue(), order.getTotalCost(), gross, net, balance);
        Client client = order.getClient() != null ? order.getClient() : order.getQuote() == null ? null : order.getQuote().getClient();
        return new Order(order, client, cancelled ? "Cancelada" : WorkOrderStatus.fieldLabel(order),
                order.getItems().stream().map(Item::of).toList(), payments, amounts);
    }

    private Profile companyProfile() {
        List<Profile> profiles = profileRepository.findAll(Sort.by("companyName", "id"));
        return profiles.stream().filter(profile -> companyImageService.companyKey(profile).filter("grupoglass"::equals).isPresent())
                .findFirst().orElseGet(() -> {
                    if (!profiles.isEmpty()) return profiles.getFirst();
                    Profile fallback = new Profile();
                    fallback.setCompanyName("Grupo Glass");
                    return fallback;
                });
    }
}
