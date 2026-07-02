package com.alfatahi.erp.service;

import com.alfatahi.erp.entity.*;
import com.alfatahi.erp.dto.ReceiptDTO;
import com.alfatahi.erp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final ReceiptHistoryRepository historyRepository;
    private final AccountsReceivableRepository receivableRepository;
    private final WorkOrderRepository workOrderRepository;
    private final ClientRepository clientRepository;
    private final AppUserRepository userRepository;
    private final ProfileRepository profileRepository;

    public ReceiptService(
            ReceiptRepository receiptRepository,
            ReceiptHistoryRepository historyRepository,
            AccountsReceivableRepository receivableRepository,
            WorkOrderRepository workOrderRepository,
            ClientRepository clientRepository,
            AppUserRepository userRepository,
            ProfileRepository profileRepository) {
        this.receiptRepository = receiptRepository;
        this.historyRepository = historyRepository;
        this.receivableRepository = receivableRepository;
        this.workOrderRepository = workOrderRepository;
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    /**
     * REGRA CRÍTICA: Só permite criar recibo se recebimento foi confirmado
     */
    public Receipt createReceipt(UUID accountsReceivableId) throws Exception {
        AccountsReceivable ar = receivableRepository.findById(accountsReceivableId)
                .orElseThrow(() -> new Exception("Conta a receber não encontrada"));

        // VERIFICAÇÃO CRÍTICA
        if (!ar.getStatus().equals("received")) {
            throw new Exception("Recibo disponível apenas após confirmação do recebimento. Status atual: " + ar.getStatus());
        }

        // Verificar se já existe recibo para esta conta
        List<Receipt> existingReceipts = receiptRepository.findByAccountsReceivable(ar);
        if (!existingReceipts.isEmpty()) {
            throw new Exception("Já existe recibo para esta conta a receber");
        }

        // Criar novo recibo
        Receipt receipt = new Receipt();
        receipt.setNumber(generateReceiptNumber());
        receipt.setAccountsReceivable(ar);
        receipt.setWorkOrder(ar.getWorkOrder());
        receipt.setClient(ar.getClient());
        receipt.setReceiptDate(LocalDate.now());
        
        // REGRA CRÍTICA: Valor do recibo é SEMPRE o valor bruto (totalAmount)
        receipt.setTotalAmount(ar.getTotalAmount());
        receipt.setPaymentMethod(ar.getPaymentMethod());
        receipt.setStatus("draft");
        receipt.setCreatedBy(getCurrentUser());

        Receipt saved = receiptRepository.save(receipt);

        // Registrar no histórico
        addHistory(saved, "CREATED", "Recibo criado automaticamente após confirmação do recebimento");

        return saved;
    }

    /**
     * Gera número único para o recibo
     * Formato: REC-YYYYMMDD-XXXX (ex: REC-20260701-0001)
     */
    private String generateReceiptNumber() {
        LocalDate today = LocalDate.now();
        String datePrefix = "REC-" + today.getYear() + String.format("%02d%02d", today.getMonthValue(), today.getDayOfMonth());
        
        // Contar recibos do dia
        List<Receipt> todayReceipts = receiptRepository.findByDateRange(today, today);
        int sequence = todayReceipts.size() + 1;
        
        return datePrefix + "-" + String.format("%04d", sequence);
    }

    /**
     * Emitir recibo (mudar status de draft para issued)
     */
    public Receipt issueReceipt(UUID receiptId) throws Exception {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new Exception("Recibo não encontrado"));

        if (!receipt.getStatus().equals("draft")) {
            throw new Exception("Apenas recibos em rascunho podem ser emitidos");
        }

        receipt.setStatus("issued");
        Receipt saved = receiptRepository.save(receipt);
        addHistory(saved, "ISSUED", "Recibo emitido");

        return saved;
    }

    /**
     * Registrar impressão
     */
    public Receipt recordPrint(UUID receiptId) throws Exception {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new Exception("Recibo não encontrado"));

        if (receipt.getStatus().equals("draft")) {
            throw new Exception("Recibo deve ser emitido antes de imprimir");
        }

        receipt.setStatus("printed");
        Receipt saved = receiptRepository.save(receipt);
        addHistory(saved, "PRINTED", "Recibo impresso");

        return saved;
    }

    /**
     * Registrar envio para cliente
     */
    public Receipt recordSent(UUID receiptId, String deliveryMethod) throws Exception {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new Exception("Recibo não encontrado"));

        receipt.setStatus("sent");
        Receipt saved = receiptRepository.save(receipt);
        addHistory(saved, "SENT", "Recibo enviado ao cliente via " + deliveryMethod);

        return saved;
    }

    /**
     * Reemitir recibo (criar novo com novo número)
     */
    public Receipt reissueReceipt(UUID receiptId, String reason) throws Exception {
        Receipt original = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new Exception("Recibo não encontrado"));

        // Criar novo recibo com mesmas informações
        Receipt reissued = new Receipt();
        reissued.setNumber(generateReceiptNumber());
        reissued.setAccountsReceivable(original.getAccountsReceivable());
        reissued.setWorkOrder(original.getWorkOrder());
        reissued.setClient(original.getClient());
        reissued.setReceiptDate(LocalDate.now());
        reissued.setTotalAmount(original.getTotalAmount());
        reissued.setPaymentMethod(original.getPaymentMethod());
        reissued.setStatus("issued");
        reissued.setNotes("Reemissão: " + reason);
        reissued.setCreatedBy(getCurrentUser());

        Receipt saved = receiptRepository.save(reissued);
        
        // Registrar no histórico do original
        addHistory(original, "REISSUED", "Recibo reenviado. Novo número: " + saved.getNumber());
        
        // Registrar no novo
        addHistory(saved, "CREATED", "Reemissão de: " + original.getNumber());

        return saved;
    }

    /**
     * Obter recibo com todos os dados necessários
     */
    @Transactional(readOnly = true)
    public ReceiptDTO getReceiptDTO(UUID receiptId) throws Exception {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new Exception("Recibo não encontrado"));

        return new ReceiptDTO(
                receipt.getId(),
                receipt.getNumber(),
                receipt.getAccountsReceivable().getId(),
                receipt.getWorkOrder().getId(),
                receipt.getClient().getId(),
                receipt.getClient().getName(),
                receipt.getClient().getDocument(),
                receipt.getClient().getAddress(),
                receipt.getWorkOrder().getNumber(),
                receipt.getWorkOrder().getDescription(),
                receipt.getReceiptDate(),
                receipt.getTotalAmount(),
                receipt.getAccountsReceivable().getReceivedAmount(),
                receipt.getPaymentMethod(),
                receipt.getStatus(),
                receipt.getResponsibleName(),
                receipt.getNotes()
        );
    }

    /**
     * Listar todos os recibos com filtros
     */
    @Transactional(readOnly = true)
    public List<ReceiptDTO> listReceipts(
            String search,
            String status,
            UUID clientId,
            LocalDate dateFrom,
            LocalDate dateTo,
            String paymentMethod) {

        List<Receipt> receipts = receiptRepository.findAllOrdered();

        // Aplicar filtros
        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            receipts = receipts.stream().filter(r ->
                    r.getNumber().toLowerCase().contains(q) ||
                    (r.getClient() != null && r.getClient().getName().toLowerCase().contains(q)) ||
                    (r.getWorkOrder() != null && r.getWorkOrder().getNumber().toLowerCase().contains(q))
            ).collect(Collectors.toList());
        }

        if (status != null && !status.isBlank()) {
            receipts = receipts.stream()
                    .filter(r -> status.equals(r.getStatus()))
                    .collect(Collectors.toList());
        }

        if (clientId != null) {
            receipts = receipts.stream()
                    .filter(r -> r.getClient() != null && clientId.equals(r.getClient().getId()))
                    .collect(Collectors.toList());
        }

        if (dateFrom != null) {
            receipts = receipts.stream()
                    .filter(r -> !r.getReceiptDate().isBefore(dateFrom))
                    .collect(Collectors.toList());
        }

        if (dateTo != null) {
            receipts = receipts.stream()
                    .filter(r -> !r.getReceiptDate().isAfter(dateTo))
                    .collect(Collectors.toList());
        }

        if (paymentMethod != null && !paymentMethod.isBlank()) {
            receipts = receipts.stream()
                    .filter(r -> paymentMethod.equals(r.getPaymentMethod()))
                    .collect(Collectors.toList());
        }

        return receipts.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * Converter entidade para DTO
     */
    private ReceiptDTO convertToDTO(Receipt receipt) {
        return new ReceiptDTO(
                receipt.getId(),
                receipt.getNumber(),
                receipt.getAccountsReceivable().getId(),
                receipt.getWorkOrder().getId(),
                receipt.getClient().getId(),
                receipt.getClient().getName(),
                receipt.getClient().getDocument(),
                receipt.getClient().getAddress(),
                receipt.getWorkOrder().getNumber(),
                receipt.getWorkOrder().getDescription(),
                receipt.getReceiptDate(),
                receipt.getTotalAmount(),
                receipt.getAccountsReceivable().getReceivedAmount(),
                receipt.getPaymentMethod(),
                receipt.getStatus(),
                receipt.getResponsibleName(),
                receipt.getNotes()
        );
    }

    /**
     * Obter histórico de um recibo
     */
    @Transactional(readOnly = true)
    public List<ReceiptHistory> getHistory(UUID receiptId) throws Exception {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new Exception("Recibo não encontrado"));

        return historyRepository.findByReceiptOrderByEventDateDesc(receipt);
    }

    /**
     * Adicionar evento ao histórico
     */
    private void addHistory(Receipt receipt, String eventType, String notes) {
        ReceiptHistory history = new ReceiptHistory();
        history.setReceipt(receipt);
        history.setEventType(eventType);
        history.setEventDate(LocalDateTime.now());
        history.setEventBy(getCurrentUser());
        history.setNotes(notes);

        historyRepository.save(history);
    }

    /**
     * Calcular KPIs
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getKPIs() {
        Map<String, Object> kpis = new HashMap<>();

        List<Receipt> allReceipts = receiptRepository.findAllOrdered();

        // Total de OS com recibo
        long totalWithReceipt = allReceipts.size();

        // Recibos emitidos
        long issued = allReceipts.stream()
                .filter(r -> r.getStatus().equals("issued") || 
                           r.getStatus().equals("printed") || 
                           r.getStatus().equals("sent") || 
                           r.getStatus().equals("reissued"))
                .count();

        // Recibos pendentes
        long pending = allReceipts.stream()
                .filter(r -> r.getStatus().equals("draft"))
                .count();

        // Valor total emitido
        BigDecimal totalValue = allReceipts.stream()
                .map(Receipt::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Recibos emitidos hoje
        LocalDate today = LocalDate.now();
        long issuedToday = allReceipts.stream()
                .filter(r -> r.getReceiptDate().equals(today) && 
                           !r.getStatus().equals("draft"))
                .count();

        kpis.put("totalWorkOrders", totalWithReceipt);
        kpis.put("issuedReceipts", issued);
        kpis.put("pendingReceipts", pending);
        kpis.put("totalValue", totalValue);
        kpis.put("issuedToday", issuedToday);
        kpis.put("recentReceipts", allReceipts.stream().limit(10).collect(Collectors.toList()));

        return kpis;
    }

    /**
     * Obter perfil da empresa para dados de impressão
     */
    @Transactional(readOnly = true)
    public Profile getCompanyProfile() {
        List<Profile> profiles = profileRepository.findAll();
        return profiles.isEmpty() ? new Profile() : profiles.get(0);
    }

    /**
     * Obter usuário atual logado
     */
    private AppUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            Optional<AppUser> user = userRepository.findByUsername(username);
            return user.orElse(null);
        }
        return null;
    }

    /**
     * Verificar se um recibo pode ser gerado para uma conta a receber
     */
    @Transactional(readOnly = true)
    public boolean canGenerateReceipt(UUID accountsReceivableId) throws Exception {
        AccountsReceivable ar = receivableRepository.findById(accountsReceivableId)
                .orElseThrow(() -> new Exception("Conta a receber não encontrada"));

        return ar.getStatus().equals("received");
    }

    /**
     * Deletar recibo (apenas se ainda em draft)
     */
    public void deleteReceipt(UUID receiptId) throws Exception {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new Exception("Recibo não encontrado"));

        if (!receipt.getStatus().equals("draft")) {
            throw new Exception("Apenas recibos em rascunho podem ser deletados");
        }

        receiptRepository.delete(receipt);
    }

    /**
     * Obter todos os recibos pendentes de uma OS
     */
    @Transactional(readOnly = true)
    public List<Receipt> getReceiptsByWorkOrder(UUID workOrderId) throws Exception {
        WorkOrder wo = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new Exception("Ordem de serviço não encontrada"));

        return receiptRepository.findByWorkOrder(wo);
    }

    /**
     * Verificar se uma OS tem recibo
     */
    @Transactional(readOnly = true)
    public boolean hasReceipt(UUID workOrderId) throws Exception {
        WorkOrder wo = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new Exception("Ordem de serviço não encontrada"));

        return !receiptRepository.findByWorkOrder(wo).isEmpty();
    }
}
