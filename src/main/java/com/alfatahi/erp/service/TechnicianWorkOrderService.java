package com.alfatahi.erp.service;

import com.alfatahi.erp.dto.TechnicianWorkOrderView;
import com.alfatahi.erp.dto.TechnicianWorkOrderView.ContractedService;
import com.alfatahi.erp.dto.TechnicianWorkOrderView.Customer;
import com.alfatahi.erp.dto.TechnicianWorkOrderView.Measurement;
import com.alfatahi.erp.dto.TechnicianWorkOrderView.Summary;
import com.alfatahi.erp.entity.*;
import com.alfatahi.erp.repository.*;
import com.alfatahi.erp.util.WorkOrderStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TechnicianWorkOrderService {
    private final WorkOrderRepository orders;
    private final ScheduleRepository schedules;
    private final WorkOrderChecklistRepository checklists;
    private final WorkOrderChecklistItemRepository items;
    private final WorkOrderExecutionNoteRepository notes;
    private final AppUserRepository users;
    private final WorkOrderMaterialSuggestions suggestions;

    public TechnicianWorkOrderService(WorkOrderRepository orders, ScheduleRepository schedules,
                                      WorkOrderChecklistRepository checklists, WorkOrderChecklistItemRepository items,
                                      WorkOrderExecutionNoteRepository notes, AppUserRepository users,
                                      WorkOrderMaterialSuggestions suggestions) {
        this.orders = orders;
        this.schedules = schedules;
        this.checklists = checklists;
        this.items = items;
        this.notes = notes;
        this.users = users;
        this.suggestions = suggestions;
    }

    public enum Filter {
        ACTIVE("Em andamento"), SCHEDULED("Agendadas"), UNSCHEDULED("Sem agendamento"), COMPLETED("Concluídas");

        private final String label;

        Filter(String label) { this.label = label; }
        public String getLabel() { return label; }
        public String getKey() { return name().toLowerCase(Locale.ROOT); }
    }

    @Transactional(readOnly = true)
    public List<Summary> list(Filter filter) {
        List<WorkOrder> listing = orders.findTechnicianListing(filter == Filter.COMPLETED,
                WorkOrderStatus.COMPLETED, WorkOrderStatus.CLOSED);
        if (listing.isEmpty()) return List.of();
        Map<UUID, Schedule> scheduleByOrder = schedules.findByWorkOrderIdIn(listing.stream().map(WorkOrder::getId).toList())
                .stream().collect(Collectors.toMap(s -> s.getWorkOrder().getId(), Function.identity()));

        return listing.stream()
                .filter(order -> {
                    boolean scheduled = installationDate(order, scheduleByOrder.get(order.getId())) != null;
                    return switch (filter) {
                        case SCHEDULED -> scheduled;
                        case UNSCHEDULED -> !scheduled;
                        default -> true;
                    };
                })
                .map(order -> summary(order, scheduleByOrder.get(order.getId()), serviceNames(order)))
                .toList();
    }

    @Transactional
    public TechnicianWorkOrderView detail(UUID orderId, boolean initialize) {
        boolean needsInitialization = initialize && !checklists.existsById(orderId);
        WorkOrder order = (needsInitialization ? orders.findByIdForFieldUpdate(orderId) : orders.findById(orderId))
                .filter(candidate -> !WorkOrderStatus.isCancelled(candidate))
                .orElseThrow(TechnicianWorkOrderService::orderNotFound);
        List<ContractedService> services = services(order);
        if (needsInitialization && WorkOrderStatus.isInProgress(order)) initializeChecklist(orderId, services);

        Schedule schedule = schedules.findByWorkOrderId(orderId).orElse(null);
        Summary summary = summary(order, schedule, services.stream().map(ContractedService::name).distinct().toList());
        String address = summary.address();
        String location = text(order.getInstallationAddress()) != null
                || order.getClient() != null && text(order.getClient().getAddress()) != null ? address : null;
        String maps = location == null ? null : "https://www.google.com/maps/search/?api=1&query="
                + URLEncoder.encode(location, StandardCharsets.UTF_8);

        String phone = firstText(order.getSitePhone(), summary.phone());
        String digits = phone == null ? "" : phone.replaceAll("\\D", "");
        String international = phone != null && phone.startsWith("+") ? digits
                : digits.length() == 10 || digits.length() == 11 ? "55" + digits : digits;
        String call = digits.length() < 8 ? null : "tel:" + (phone.startsWith("+") ? "+" : "") + digits;
        String whatsapp = international.matches("[1-9]\\d{9,14}") ? "https://wa.me/" + international : null;

        Client client = order.getClient();
        Customer customer = client == null ? null : new Customer(text(client.getName()), text(client.getDocument()),
                text(client.getPhone()), text(client.getEmail()), text(client.getAddress()), text(client.getCity()),
                text(client.getNotes()));

        return new TechnicianWorkOrderView(summary, order.getCreatedAt(),
                schedule == null || summary.installationDate() == null ? null : schedule.getScheduledTime(),
                firstText(order.getTechnicianName(), schedule == null ? null : schedule.getResponsible()),
                schedule == null ? null : text(schedule.getTeam()), text(order.getDescription()), text(order.getNotes()),
                text(order.getInstallationNotes()), schedule == null ? null : text(schedule.getObservations()),
                text(order.getSiteContact()), text(order.getSitePhone()), text(order.getAccessInstructions()), maps, call, whatsapp, customer,
                services, items.findByWorkOrderIdOrderByCompletedAscCreatedAtAscIdAsc(orderId),
                notes.findByWorkOrderIdOrderByCreatedAtDescIdDesc(orderId));
    }

    @Transactional
    public void addItem(UUID orderId, String name, BigDecimal quantity, String observation) {
        WorkOrder order = editableOrder(orderId);
        if (!checklists.existsById(orderId)) initializeChecklist(orderId, services(order));
        items.save(new WorkOrderChecklistItem(orderId, name, quantity, observation, null));
    }

    @Transactional
    public boolean editItem(UUID orderId, UUID itemId, long version, String name, BigDecimal quantity, String observation) {
        WorkOrderChecklistItem item = editableItem(orderId, itemId);
        if (item.getVersion() != version) return false;
        item.update(name, quantity, observation);
        return true;
    }

    @Transactional
    public boolean markItem(UUID orderId, UUID itemId, long version, boolean completed) {
        WorkOrderChecklistItem item = editableItem(orderId, itemId);
        if (item.isCompleted() == completed) return true;
        if (item.getVersion() != version) return false;
        item.setCompleted(completed);
        return true;
    }

    @Transactional
    public boolean deleteItem(UUID orderId, UUID itemId, long version) {
        WorkOrderChecklistItem item = editableItem(orderId, itemId);
        if (item.getVersion() != version) return false;
        items.delete(item);
        return true;
    }

    @Transactional
    public int generateSuggestions(UUID orderId) {
        WorkOrder order = editableOrder(orderId);
        createChecklistIfMissing(orderId);
        return addSuggestions(orderId, services(order));
    }

    @Transactional
    public void addNote(UUID orderId, String username, String body) {
        editableOrder(orderId);
        AppUser author = users.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        notes.save(new WorkOrderExecutionNote(orderId, author, body));
    }

    private WorkOrder editableOrder(UUID orderId) {
        WorkOrder order = orders.findByIdForFieldUpdate(orderId).filter(candidate -> !WorkOrderStatus.isCancelled(candidate))
                .orElseThrow(TechnicianWorkOrderService::orderNotFound);
        if (WorkOrderStatus.isCompleted(order)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return order;
    }

    private WorkOrderChecklistItem editableItem(UUID orderId, UUID itemId) {
        editableOrder(orderId);
        return items.findByWorkOrderIdAndId(orderId, itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private static ResponseStatusException orderNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Ordem de serviço indisponível.");
    }

    private void initializeChecklist(UUID orderId, List<ContractedService> services) {
        if (createChecklistIfMissing(orderId)) addSuggestions(orderId, services);
    }

    private boolean createChecklistIfMissing(UUID orderId) {
        if (checklists.existsById(orderId)) return false;
        checklists.saveAndFlush(new WorkOrderChecklist(orderId));
        return true;
    }

    private int addSuggestions(UUID orderId, List<ContractedService> services) {
        List<WorkOrderChecklistItem> current = items.findByWorkOrderIdOrderByCompletedAscCreatedAtAscIdAsc(orderId);
        Set<String> keys = current.stream().map(WorkOrderChecklistItem::getSuggestionKey)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> names = current.stream().map(item -> WorkOrderMaterialSuggestions.normalize(item.getName()))
                .collect(Collectors.toSet());
        List<WorkOrderChecklistItem> added = new ArrayList<>();
        for (var service : services) {
            for (var material : suggestions.forService(service)) {
                String name = WorkOrderMaterialSuggestions.normalize(material.name());
                String key = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)).toString();
                if (keys.add(key) && names.add(name)) {
                    added.add(new WorkOrderChecklistItem(orderId, material.name(), null, material.observation(), key));
                }
            }
        }
        items.saveAll(added);
        return added.size();
    }

    private static Summary summary(WorkOrder order, Schedule schedule, List<String> serviceNames) {
        Client client = order.getClient();
        String address = firstText(order.getInstallationAddress(),
                client == null ? null : joinAddress(client.getAddress(), client.getCity()));
        return new Summary(order.getId(), order.getNumber(), client == null ? null : text(client.getName()), address,
                firstText(client == null ? null : client.getPhone(), order.getSitePhone()), serviceNames,
                installationDate(order, schedule), WorkOrderStatus.fieldLabel(order), WorkOrderStatus.isCompleted(order));
    }

    private static LocalDate installationDate(WorkOrder order, Schedule schedule) {
        if (schedule == null) return order.getInstallDate();
        return Schedule.STATUS_CANCELADO.equals(schedule.getStatus()) ? null : schedule.getScheduledDate();
    }

    private record SoldLine(QuoteItem quote, WorkOrderItem item) {
        String name() {
            return firstText(quote == null ? null : quote.getProduct(), quote == null ? null : quote.getCategory(),
                    item == null ? null : item.getDescription(), "Serviço");
        }
    }

    private static List<String> serviceNames(WorkOrder order) {
        List<SoldLine> sold = soldLines(order);
        if (!sold.isEmpty()) return sold.stream().map(SoldLine::name).distinct().toList();
        String title = firstText(order.getTitle(), order.getCategory() == null ? null : order.getCategory().getName());
        return title == null ? List.of() : List.of(title);
    }

    private static List<SoldLine> soldLines(WorkOrder order) {
        List<QuoteItem> quoteItems = order.getQuote() == null ? List.of() : order.getQuote().getItems();
        Map<UUID, QuoteItem> byId = quoteItems.stream().filter(item -> item.getId() != null)
                .collect(Collectors.toMap(QuoteItem::getId, Function.identity()));
        Set<QuoteItem> used = new HashSet<>();
        List<SoldLine> result = new ArrayList<>();
        for (WorkOrderItem item : order.getItems()) {
            if (item.getSourceExpenseAllocationId() != null) continue;
            QuoteItem quoteItem = byId.get(item.getQuoteItemId());
            if (quoteItem == null && item.getQuoteItemId() == null) {
                quoteItem = legacyQuoteItem(item, quoteItems, used);
            }
            if (quoteItem != null) used.add(quoteItem);
            result.add(new SoldLine(quoteItem, item));
        }
        for (QuoteItem quoteItem : quoteItems) {
            if (!used.contains(quoteItem)) result.add(new SoldLine(quoteItem, null));
        }
        return result;
    }

    private static List<ContractedService> services(WorkOrder order) {
        List<SoldLine> sold = soldLines(order);
        if (!sold.isEmpty()) return sold.stream().map(TechnicianWorkOrderService::service).toList();
        String category = order.getCategory() == null ? null : text(order.getCategory().getName());
        String title = firstText(order.getTitle(), category);
        if (title == null) return List.of();
        return List.of(new ContractedService(title, category, text(order.getDescription()), null,
                singleMeasurement(order.getWidth(), order.getHeight()), null, null));
    }

    private static QuoteItem legacyQuoteItem(WorkOrderItem item, List<QuoteItem> quoteItems, Set<QuoteItem> used) {
        String label = WorkOrderMaterialSuggestions.normalize(item.getDescription());
        if (label.isEmpty()) return null;
        List<QuoteItem> matching = quoteItems.stream().filter(q -> !used.contains(q))
                .filter(q -> label.equals(WorkOrderMaterialSuggestions.normalize(q.getProduct()))
                        || label.equals(WorkOrderMaterialSuggestions.normalize(
                        firstText(q.getCategory(), "Item") + " - " + firstText(q.getProduct(), "Sem descrição"))))
                .limit(2).toList();
        return matching.size() == 1 ? matching.getFirst() : null;
    }

    private static ContractedService service(SoldLine line) {
        QuoteItem quote = line.quote();
        WorkOrderItem item = line.item();
        if (quote == null) {
            return new ContractedService(line.name(), null, null, item.getQuantity(), List.of(),
                    text(item.getTechnicalMeasurements()), text(item.getTechnicalNotes()));
        }
        boolean perMeasurement = quote.hasPerMeasurementPricing();
        List<Measurement> measurements = quote.getMeasurements().isEmpty()
                ? singleMeasurement(quote.getWidth(), quote.getHeight())
                : quote.getMeasurements().stream().map(m -> new Measurement(m.getWidth(), m.getHeight(),
                        m.getQuantity() != null ? m.getQuantity() : perMeasurement ? quote.getQuantity() : null)).toList();
        String fieldMeasurements = item == null ? null : text(item.getTechnicalMeasurements());
        return new ContractedService(line.name(),
                text(quote.getCategory()), text(quote.getDescription()),
                perMeasurement ? null : quote.getQuantity(), measurements, fieldMeasurements,
                item == null ? null : text(item.getTechnicalNotes()));
    }

    private static List<Measurement> singleMeasurement(BigDecimal width, BigDecimal height) {
        return width != null && width.signum() > 0 && height != null && height.signum() > 0
                ? List.of(new Measurement(width, height, null)) : List.of();
    }

    private static String joinAddress(String address, String city) {
        if (text(address) == null) return text(city);
        return text(city) == null ? text(address) : address.strip() + " – " + city.strip();
    }

    private static String firstText(String... values) {
        return Arrays.stream(values).map(TechnicianWorkOrderService::text).filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    private static String text(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
