package com.alfatahi.erp.service;

import com.alfatahi.erp.dto.ReceiptSaveRequest;
import com.alfatahi.erp.entity.*;
import com.alfatahi.erp.repository.ClientRepository;
import com.alfatahi.erp.repository.ProfileRepository;
import com.alfatahi.erp.repository.ReceiptRepository;
import com.alfatahi.erp.repository.WorkOrderRepository;
import com.alfatahi.erp.service.CompanyImageService.Kind;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ReceiptService {

    public static final int MAX_PHOTOS = 12;
    public static final int MAX_PHOTO_BYTES = 6 * 1024 * 1024;
    private static final Set<String> ALLOWED_PHOTO_TYPES = Set.of("image/jpeg", "image/png");

    private final ReceiptRepository receiptRepo;
    private final ProfileRepository profileRepo;
    private final ClientRepository clientRepo;
    private final WorkOrderRepository workOrderRepo;
    private final CompanyImageService companyImageService;

    public ReceiptService(ReceiptRepository receiptRepo, ProfileRepository profileRepo,
                          ClientRepository clientRepo, WorkOrderRepository workOrderRepo,
                          CompanyImageService companyImageService) {
        this.receiptRepo = receiptRepo;
        this.profileRepo = profileRepo;
        this.clientRepo = clientRepo;
        this.workOrderRepo = workOrderRepo;
        this.companyImageService = companyImageService;
    }


    @Transactional
    public Receipt save(ReceiptSaveRequest req, String username) {
        if (req == null) throw new IllegalArgumentException("Requisição inválida.");

        if (req.profileId() == null) throw new IllegalArgumentException("Selecione a empresa emissora.");
        Profile profile = profileRepo.findById(req.profileId())
                .orElseThrow(() -> new IllegalArgumentException("Empresa emissora não encontrada."));

        WorkOrder workOrder = null;
        if (req.workOrderId() != null) {
            workOrder = workOrderRepo.findById(req.workOrderId())
                    .orElseThrow(() -> new IllegalArgumentException("Ordem de serviço não encontrada."));
        }

        Client client = null;
        if (workOrder != null && workOrder.getClient() != null) {
            client = workOrder.getClient();
        } else if (req.clientId() != null) {
            client = clientRepo.findById(req.clientId())
                    .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));
        }
        if (client == null) throw new IllegalArgumentException("Selecione o cliente.");

        List<ReceiptItem> newItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        if (req.items() != null) {
            for (ReceiptSaveRequest.Item in : req.items()) {
                if (in == null || in.description() == null || in.description().isBlank()) continue;
                BigDecimal qty = in.quantity() != null ? in.quantity() : BigDecimal.ONE;
                BigDecimal price = in.unitPrice() != null ? in.unitPrice() : BigDecimal.ZERO;
                if (qty.signum() <= 0) throw new IllegalArgumentException("A quantidade dos serviços deve ser maior que zero.");
                if (price.signum() < 0) throw new IllegalArgumentException("O valor dos serviços não pode ser negativo.");
                ReceiptItem item = new ReceiptItem();
                item.setDescription(in.description().trim());
                item.setQuantity(qty.setScale(2, RoundingMode.HALF_UP));
                item.setUnitPrice(price.setScale(2, RoundingMode.HALF_UP));
                item.setSortOrder(newItems.size());
                subtotal = subtotal.add(item.getSubtotal());
                newItems.add(item);
            }
        }
        if (newItems.isEmpty()) throw new IllegalArgumentException("Adicione pelo menos um serviço ao recibo.");

        BigDecimal discount = req.discount() != null ? req.discount() : BigDecimal.ZERO;
        discount = discount.setScale(2, RoundingMode.HALF_UP);
        if (discount.signum() < 0) throw new IllegalArgumentException("O desconto não pode ser negativo.");
        if (discount.compareTo(subtotal) > 0) {
            throw new IllegalArgumentException("O desconto não pode ser maior que o valor dos serviços.");
        }

        Receipt receipt;
        if (req.id() != null) {
            receipt = receiptRepo.findById(req.id())
                    .orElseThrow(() -> new IllegalArgumentException("Recibo não encontrado."));
        } else {
            receipt = new Receipt();
            receipt.setNumber(String.format("REC-%04d", receiptRepo.nextReceiptSequence()));
            receipt.setCreatedByName(username);
        }

        LocalDate today = FinancialPeriod.today();
        LocalDate issueDate = req.issueDate() != null ? req.issueDate() : today;
        LocalDate receivedDate = req.receivedDate() != null ? req.receivedDate() : issueDate;

        receipt.setProfile(profile);
        receipt.setClient(client);
        receipt.setWorkOrder(workOrder);
        receipt.setIssueDate(issueDate);
        receipt.setReceivedDate(receivedDate);
        receipt.setDiscount(discount);
        receipt.setTotalAmount(subtotal.subtract(discount).setScale(2, RoundingMode.HALF_UP));
        receipt.setPaymentTerms(blankToNull(req.paymentTerms()));
        receipt.setWarranty(blankToNull(req.warranty()));
        receipt.setDescription(blankToNull(req.description()));

        receipt.getItems().clear();
        for (ReceiptItem item : newItems) {
            item.setReceipt(receipt);
            receipt.getItems().add(item);
        }

        applyPhotos(receipt, req.photos());

        return receiptRepo.save(receipt);
    }

    private void applyPhotos(Receipt receipt, List<ReceiptSaveRequest.Photo> requested) {
        List<ReceiptSaveRequest.Photo> photos = requested != null ? requested : List.of();
        if (photos.size() > MAX_PHOTOS) {
            throw new IllegalArgumentException("Limite de " + MAX_PHOTOS + " fotos por recibo.");
        }

        Set<UUID> keep = new HashSet<>();
        for (ReceiptSaveRequest.Photo p : photos) {
            if (p != null && p.id() != null) keep.add(p.id());
        }
        receipt.getPhotos().removeIf(existing -> existing.getId() == null || !keep.contains(existing.getId()));

        Map<UUID, ReceiptPhoto> existingById = new HashMap<>();
        for (ReceiptPhoto existing : receipt.getPhotos()) existingById.put(existing.getId(), existing);

        int order = 0;
        for (ReceiptSaveRequest.Photo p : photos) {
            if (p == null) continue;
            ReceiptPhoto existing = p.id() != null ? existingById.get(p.id()) : null;
            if (existing != null) {
                existing.setSortOrder(order++);
                continue;
            }
            if (p.dataUri() == null || p.dataUri().isBlank()) continue;
            ReceiptPhoto photo = decodePhoto(p.dataUri());
            photo.setReceipt(receipt);
            photo.setSortOrder(order);
            photo.setFileName("foto-" + (order + 1) + ("image/png".equals(photo.getContentType()) ? ".png" : ".jpg"));
            receipt.getPhotos().add(photo);
            order++;
        }
    }

    private ReceiptPhoto decodePhoto(String dataUri) {
        String trimmed = dataUri.trim();
        int marker = trimmed.indexOf(";base64,");
        if (!trimmed.startsWith("data:") || marker < 0) {
            throw new IllegalArgumentException("Formato de foto inválido.");
        }
        String mime = trimmed.substring(5, marker).toLowerCase(Locale.ROOT);
        if (!ALLOWED_PHOTO_TYPES.contains(mime)) {
            throw new IllegalArgumentException("Use fotos nos formatos JPG ou PNG.");
        }
        byte[] bytes;
        try {
            bytes = Base64.getMimeDecoder().decode(trimmed.substring(marker + 8));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Não foi possível ler uma das fotos enviadas.");
        }
        if (bytes.length == 0 || bytes.length > MAX_PHOTO_BYTES) {
            throw new IllegalArgumentException("Cada foto deve ter no máximo 6 MB.");
        }
        if (!looksLikeImage(bytes, mime)) {
            throw new IllegalArgumentException("Uma das fotos enviadas não é uma imagem válida.");
        }
        ReceiptPhoto photo = new ReceiptPhoto();
        photo.setContentType(mime);
        photo.setContent(bytes);
        return photo;
    }

    private static boolean looksLikeImage(byte[] b, String mime) {
        if ("image/jpeg".equals(mime)) {
            return b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8;
        }
        return b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
    }

    @Transactional
    public void delete(UUID id) {
        receiptRepo.findById(id).ifPresent(receiptRepo::delete);
    }


    @Transactional(readOnly = true)
    public Map<String, Object> view(UUID id) {
        Receipt r = receiptRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recibo não encontrado."));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", r.getId());
        out.put("number", r.getNumber());
        out.put("issueDate", r.getIssueDate() != null ? r.getIssueDate().toString() : null);
        out.put("receivedDate", r.getReceivedDate() != null ? r.getReceivedDate().toString() : null);
        out.put("profileId", r.getProfile() != null ? r.getProfile().getId() : null);
        out.put("profile", profileMap(r.getProfile()));
        out.put("client", clientMap(r.getClient()));
        if (r.getWorkOrder() != null) {
            Map<String, Object> wo = new LinkedHashMap<>();
            wo.put("id", r.getWorkOrder().getId());
            wo.put("number", r.getWorkOrder().getNumber());
            out.put("workOrder", wo);
        } else {
            out.put("workOrder", null);
        }

        List<Map<String, Object>> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ReceiptItem item : r.getItems()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("description", item.getDescription());
            m.put("quantity", item.getQuantity());
            m.put("unitPrice", item.getUnitPrice());
            m.put("subtotal", item.getSubtotal());
            items.add(m);
            subtotal = subtotal.add(item.getSubtotal());
        }
        out.put("items", items);
        out.put("publicToken", r.getPublicToken());
        out.put("signed", r.isSigned());
        out.put("clientSignature", signatureDataUri(r.getClientSignature()));
        out.put("seller", r.getCreatedByName());
        out.put("subtotal", subtotal);
        out.put("discount", r.getDiscount());
        out.put("total", r.getTotalAmount());
        out.put("paymentTerms", r.getPaymentTerms());
        out.put("warranty", r.getWarranty());
        out.put("description", r.getDescription());

        List<Map<String, Object>> photos = new ArrayList<>();
        for (ReceiptPhoto p : r.getPhotos()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("url", "/receipts/" + r.getId() + "/photos/" + p.getId());
            photos.add(m);
        }
        out.put("photos", photos);
        return out;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> workOrderData(UUID id) {
        WorkOrder wo = workOrderRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ordem de serviço não encontrada."));

        List<Map<String, Object>> items = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO;
        for (WorkOrderItem item : wo.getItems()) {
            if (item.getSourceExpenseAllocationId() != null) continue;
            BigDecimal price = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
            if (price.signum() <= 0) continue;
            BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ONE;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("description", item.getDescription());
            m.put("quantity", qty);
            m.put("unitPrice", price);
            items.add(m);
            sum = sum.add(qty.multiply(price));
        }

        BigDecimal total = wo.getTotalValue();
        if (items.isEmpty() && total.signum() > 0) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("description", "Serviço conforme ordem de serviço " + nvl(wo.getNumber()));
            m.put("quantity", BigDecimal.ONE);
            m.put("unitPrice", total);
            items.add(m);
            sum = total;
        }
        BigDecimal discount = (total.signum() > 0 && sum.compareTo(total) > 0) ? sum.subtract(total) : BigDecimal.ZERO;

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", wo.getId());
        out.put("number", wo.getNumber());
        out.put("total", total);
        out.put("client", clientMap(wo.getClient()));
        out.put("items", items);
        out.put("discount", discount.setScale(2, RoundingMode.HALF_UP));
        out.put("existingReceipts", receiptRepo.countByWorkOrderId(wo.getId()));

        Quote quote = wo.getQuote();
        String payment = null;
        String warranty = null;
        if (quote != null) {
            warranty = quote.getWarranty();
            payment = quote.getPaymentMethod();
            if (payment != null && !payment.isBlank()
                    && quote.getInstallments() != null && quote.getInstallments() > 1
                    && (payment.contains("Crédito") || payment.contains("Link de Pagamento"))) {
                payment = payment + " (em até " + quote.getInstallments() + "x)";
            }
        }
        out.put("paymentTerms", payment);
        out.put("warranty", warranty);
        return out;
    }


    @Transactional(readOnly = true)
    public List<Map<String, Object>> selectableWorkOrders() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (WorkOrder wo : workOrderRepo.findAllByOrderByCreatedAtDesc()) {
            String status = wo.getStatus();
            if ("cancelled".equalsIgnoreCase(status) || "canceled".equalsIgnoreCase(status)) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", wo.getId());
            m.put("number", wo.getNumber());
            m.put("clientName", wo.getClient() != null ? wo.getClient().getName() : "");
            m.put("total", wo.getTotalValue());
            m.put("status", status);
            m.put("date", wo.getCreatedAt() != null
                    ? wo.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
            out.add(m);
        }
        return out;
    }




    public record PhotoData(String contentType, byte[] content) { }

    private static final int MAX_SIGNATURE_CHARS = 900_000;
    private static final String PNG_PREFIX = "data:image/png;base64,";

    @Transactional(readOnly = true)
    public Map<String, Object> publicView(String token) {
        Receipt r = receiptRepo.findByPublicToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Recibo não encontrado ou link inválido."));

        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        BigDecimal gross = BigDecimal.ZERO;
        List<Map<String, Object>> items = new ArrayList<>();
        for (ReceiptItem item : r.getItems()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("quantity", item.getQuantity().stripTrailingZeros().toPlainString());
            m.put("description", item.getDescription());
            m.put("subtotal", formatBRL(item.getSubtotal()));
            items.add(m);
            gross = gross.add(item.getSubtotal());
        }
        List<String> photos = new ArrayList<>();
        for (ReceiptPhoto p : r.getPhotos()) {
            photos.add("/public/receipts/" + token + "/photos/" + p.getId());
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("number", r.getNumber());
        out.put("token", r.getPublicToken());
        out.put("clientName", r.getClient() != null ? nvl(r.getClient().getName()) : "");
        out.put("companyName", r.getProfile() != null ? nvl(r.getProfile().getCompanyName()) : "");
        out.put("issueDate", r.getIssueDate() != null ? r.getIssueDate().format(df) : "");
        out.put("receivedDate", r.getReceivedDate() != null ? r.getReceivedDate().format(df) : "");
        out.put("items", items);
        out.put("hasDiscount", r.getDiscount().signum() > 0);
        out.put("gross", formatBRL(gross));
        out.put("discount", formatBRL(r.getDiscount()));
        out.put("total", formatBRL(r.getTotalAmount()));
        out.put("description", nvl(r.getDescription()));
        out.put("paymentTerms", nvl(r.getPaymentTerms()));
        out.put("warranty", nvl(r.getWarranty()));
        out.put("photos", photos);
        out.put("signed", r.isSigned());
        out.put("clientSignature", signatureDataUri(r.getClientSignature()));
        return out;
    }

    @Transactional
    public void sign(String token, String signature) {
        Receipt r = receiptRepo.findByPublicToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Recibo não encontrado ou link inválido."));
        if (r.isSigned()) throw new IllegalStateException("Este recibo já foi assinado.");

        String sig = signature == null ? "" : signature.trim();
        if (!sig.startsWith(PNG_PREFIX) || sig.length() > MAX_SIGNATURE_CHARS) {
            throw new IllegalArgumentException("Assinatura inválida.");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(sig.substring(PNG_PREFIX.length()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Assinatura inválida.");
        }
        if (!looksLikeImage(bytes, "image/png")) throw new IllegalArgumentException("Assinatura inválida.");

        r.setClientSignature(sig);
        r.setClientSignedAt(java.time.LocalDateTime.now());
        receiptRepo.save(r);
    }

    @Transactional
    public void resetSignature(UUID id) {
        Receipt r = receiptRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recibo não encontrado."));
        r.setClientSignature(null);
        r.setClientSignedAt(null);
        receiptRepo.save(r);
    }

    @Transactional(readOnly = true)
    public Optional<PhotoData> publicPhoto(String token, UUID photoId) {
        return receiptRepo.findByPublicToken(token).flatMap(r -> r.getPhotos().stream()
                .filter(p -> photoId.equals(p.getId()))
                .findFirst()
                .map(p -> new PhotoData(p.getContentType(), p.getContent())));
    }

    public static String signatureDataUri(String signature) {
        if (signature == null || signature.isBlank()) return null;
        String t = signature.trim();
        return t.startsWith("data:image/") ? t : null;
    }

    public static String[] splitDescription(String description) {
        String d = description == null ? "" : description.trim();
        int nl = d.indexOf('\n');
        if (nl > 0) return new String[]{d.substring(0, nl).trim(), d.substring(nl + 1).trim()};
        int dash = d.indexOf(" - ");
        if (dash > 0) return new String[]{d.substring(0, dash).trim(), d.substring(dash + 3).trim()};
        return new String[]{d, ""};
    }

    public static String formatBRL(BigDecimal v) {
        return java.text.NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(v != null ? v : BigDecimal.ZERO);
    }

    public Map<String, Object> profileMap(Profile p) {
        if (p == null) return null;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("companyName", nvl(p.getCompanyName()));
        m.put("document", nvl(p.getDocument()));
        m.put("address", nvl(p.getAddress()));
        m.put("email", nvl(p.getEmail()));
        m.put("phone", nvl(p.getPhone()));
        String base = "/quotes/company-image/" + p.getId();
        m.put("logoUrl", companyImageService.hasImage(p, Kind.LOGO) ? base + "/logo" : null);
        m.put("signatureUrl", companyImageService.hasImage(p, Kind.SIGNATURE) ? base + "/signature" : null);
        return m;
    }

    public Map<String, Object> clientMap(Client c) {
        if (c == null) return null;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("name", nvl(c.getName()));
        m.put("document", nvl(c.getDocument()));
        m.put("phone", nvl(c.getPhone()));
        m.put("email", nvl(c.getEmail()));
        m.put("city", nvl(c.getCity()));
        m.put("address", fullAddress(c));
        return m;
    }

    public static String fullAddress(Client c) {
        if (c == null) return "";
        String addr = nvl(c.getAddress());
        String city = nvl(c.getCity());
        return addr + (!addr.isEmpty() && !city.isEmpty() ? " - " : "") + city;
    }

    private static String nvl(String v) { return v != null ? v : ""; }

    private static String blankToNull(String v) { return (v == null || v.isBlank()) ? null : v.trim(); }
}
