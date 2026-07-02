package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.Receipt;
import com.alfatahi.erp.entity.ReceiptHistory;
import com.alfatahi.erp.dto.ReceiptDTO;
import com.alfatahi.erp.service.ReceiptService;
import com.alfatahi.erp.service.ClientService;
import com.alfatahi.erp.service.WorkOrderService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/receipts")
public class ReceiptController {

    private final ReceiptService receiptService;
    private final ClientService clientService;
    private final WorkOrderService workOrderService;

    public ReceiptController(ReceiptService receiptService,
                            ClientService clientService,
                            WorkOrderService workOrderService) {
        this.receiptService = receiptService;
        this.clientService = clientService;
        this.workOrderService = workOrderService;
    }

    /**
     * GET /receipts - Tela principal de recibos
     */
    @GetMapping
    @Transactional(readOnly = true)
    public String index(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String paymentMethod,
            Model model) {

        try {
            // Buscar recibos com filtros
            List<ReceiptDTO> receipts = receiptService.listReceipts(
                    search, status, clientId, dateFrom, dateTo, paymentMethod);

            // Obter KPIs
            Map<String, Object> kpis = receiptService.getKPIs();

            // Adicionar dados ao modelo
            model.addAttribute("currentPage", "receipts");
            model.addAttribute("receipts", receipts);
            model.addAttribute("clients", clientService.listAllActive());

            // KPIs
            model.addAttribute("totalWorkOrders", kpis.get("totalWorkOrders"));
            model.addAttribute("issuedReceipts", kpis.get("issuedReceipts"));
            model.addAttribute("pendingReceipts", kpis.get("pendingReceipts"));
            model.addAttribute("totalValue", kpis.get("totalValue"));
            model.addAttribute("issuedToday", kpis.get("issuedToday"));

            // Filtros (para manter valores nos campos)
            model.addAttribute("filterSearch", search);
            model.addAttribute("filterStatus", status);
            model.addAttribute("filterClientId", clientId);
            model.addAttribute("filterDateFrom", dateFrom);
            model.addAttribute("filterDateTo", dateTo);
            model.addAttribute("filterPaymentMethod", paymentMethod);

        } catch (Exception e) {
            model.addAttribute("error", "Erro ao carregar recibos: " + e.getMessage());
        }

        return "receipts";
    }

    /**
     * GET /receipts/{id} - Obter dados de um recibo (JSON)
     */
    @GetMapping("/{id}")
    @ResponseBody
    @Transactional(readOnly = true)
    public Map<String, Object> getReceipt(@PathVariable UUID id) {
        try {
            ReceiptDTO receipt = receiptService.getReceiptDTO(id);
            List<ReceiptHistory> history = receiptService.getHistory(id);

            return Map.of(
                    "success", true,
                    "receipt", receipt,
                    "history", history
            );
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
        }
    }

    /**
     * POST /receipts/create - Criar novo recibo
     */
    @PostMapping("/create")
    public String createReceipt(
            @RequestParam UUID accountsReceivableId,
            RedirectAttributes redirectAttributes) {

        try {
            Receipt receipt = receiptService.createReceipt(accountsReceivableId);
            redirectAttributes.addFlashAttribute("success", 
                    "Recibo " + receipt.getNumber() + " criado com sucesso!");
            return "redirect:/receipts?highlightId=" + receipt.getId();

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                    "Erro ao criar recibo: " + e.getMessage());
            return "redirect:/receivables";
        }
    }

    /**
     * POST /receipts/{id}/issue - Emitir recibo
     */
    @PostMapping("/{id}/issue")
    public String issueReceipt(
            @PathVariable UUID id,
            RedirectAttributes redirectAttributes) {

        try {
            Receipt receipt = receiptService.issueReceipt(id);
            redirectAttributes.addFlashAttribute("success", 
                    "Recibo " + receipt.getNumber() + " emitido com sucesso!");
            return "redirect:/receipts";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                    "Erro ao emitir recibo: " + e.getMessage());
            return "redirect:/receipts";
        }
    }

    /**
     * POST /receipts/{id}/print - Registrar impressão
     */
    @PostMapping("/{id}/print")
    public String recordPrint(
            @PathVariable UUID id,
            RedirectAttributes redirectAttributes) {

        try {
            Receipt receipt = receiptService.recordPrint(id);
            redirectAttributes.addFlashAttribute("success", 
                    "Impressão do recibo " + receipt.getNumber() + " registrada!");
            return "redirect:/receipts";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                    "Erro ao registrar impressão: " + e.getMessage());
            return "redirect:/receipts";
        }
    }

    /**
     * POST /receipts/{id}/send - Registrar envio
     */
    @PostMapping("/{id}/send")
    public String recordSent(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "EMAIL") String deliveryMethod,
            RedirectAttributes redirectAttributes) {

        try {
            Receipt receipt = receiptService.recordSent(id, deliveryMethod);
            redirectAttributes.addFlashAttribute("success", 
                    "Envio do recibo " + receipt.getNumber() + " registrado!");
            return "redirect:/receipts";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                    "Erro ao registrar envio: " + e.getMessage());
            return "redirect:/receipts";
        }
    }

    /**
     * POST /receipts/{id}/reissue - Reemitir recibo
     */
    @PostMapping("/{id}/reissue")
    public String reissueReceipt(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "Solicitação do cliente") String reason,
            RedirectAttributes redirectAttributes) {

        try {
            Receipt newReceipt = receiptService.reissueReceipt(id, reason);
            redirectAttributes.addFlashAttribute("success", 
                    "Recibo reenviado. Novo número: " + newReceipt.getNumber());
            return "redirect:/receipts";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                    "Erro ao reemitir recibo: " + e.getMessage());
            return "redirect:/receipts";
        }
    }

    /**
     * POST /receipts/{id}/delete - Deletar recibo
     */
    @PostMapping("/{id}/delete")
    public String deleteReceipt(
            @PathVariable UUID id,
            RedirectAttributes redirectAttributes) {

        try {
            receiptService.deleteReceipt(id);
            redirectAttributes.addFlashAttribute("success", "Recibo deletado com sucesso!");
            return "redirect:/receipts";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                    "Erro ao deletar recibo: " + e.getMessage());
            return "redirect:/receipts";
        }
    }

    /**
     * GET /receipts/{id}/pdf - Gerar PDF do recibo
     */
    @GetMapping("/{id}/pdf")
    public String generatePDF(
            @PathVariable UUID id,
            RedirectAttributes redirectAttributes) {

        try {
            ReceiptDTO receipt = receiptService.getReceiptDTO(id);
            
            // TODO: Implementar geração de PDF
            // Por enquanto, apenas registrar visualização
            
            redirectAttributes.addFlashAttribute("info", 
                    "PDF do recibo " + receipt.getNumber() + " será gerado em breve");
            return "redirect:/receipts";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                    "Erro ao gerar PDF: " + e.getMessage());
            return "redirect:/receipts";
        }
    }

    /**
     * GET /receipts/can-generate/{accountsReceivableId} - Verificar se pode gerar recibo (AJAX)
     */
    @GetMapping("/can-generate/{accountsReceivableId}")
    @ResponseBody
    public Map<String, Object> canGenerateReceipt(@PathVariable UUID accountsReceivableId) {
        try {
            boolean canGenerate = receiptService.canGenerateReceipt(accountsReceivableId);
            return Map.of(
                    "success", true,
                    "canGenerate", canGenerate,
                    "message", canGenerate ? "Recibo pode ser gerado" : "Aguardando confirmação do recebimento"
            );
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
        }
    }
}
