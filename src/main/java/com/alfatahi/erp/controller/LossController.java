package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.AccountsPayable;
import com.alfatahi.erp.entity.AccountsReceivable;
import com.alfatahi.erp.entity.Loss;
import com.alfatahi.erp.repository.AccountsPayableRepository;
import com.alfatahi.erp.repository.AccountsReceivableRepository;
import com.alfatahi.erp.repository.LossRepository;
import com.alfatahi.erp.service.WorkOrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/losses")
public class LossController {

    private final LossRepository lossRepo;
    private final WorkOrderService workOrderService;
    private final AccountsReceivableRepository recRepo;
    private final AccountsPayableRepository payRepo;

    public LossController(LossRepository lossRepo, WorkOrderService workOrderService,
                          AccountsReceivableRepository recRepo, AccountsPayableRepository payRepo) {
        this.lossRepo = lossRepo;
        this.workOrderService = workOrderService;
        this.recRepo = recRepo;
        this.payRepo = payRepo;
    }

    @GetMapping
    public String index(Model model) {
        List<Loss> losses = lossRepo.findAllByOrderByOccurrenceDateDesc();

        // ==========================================
        // CÁLCULOS DE INDICADORES GERENCIAIS
        // ==========================================
        BigDecimal totalLoss = losses.stream().map(Loss::getFinancialImpact).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRevenue = recRepo.findAll().stream().map(AccountsReceivable::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCosts = payRepo.findAll().stream().map(AccountsPayable::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pctRevenue = totalRevenue.compareTo(BigDecimal.ZERO) > 0 ?
                totalLoss.multiply(new BigDecimal("100")).divide(totalRevenue, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        BigDecimal pctCosts = totalCosts.compareTo(BigDecimal.ZERO) > 0 ?
                totalLoss.multiply(new BigDecimal("100")).divide(totalCosts, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        // Sub-categorias
        BigDecimal quebra = sumByType(losses, "Quebra de Vidro");
        BigDecimal corte = sumByType(losses, "Erro de Corte");
        BigDecimal retrabalho = sumByType(losses, "Retrabalho");
        BigDecimal transporte = sumByType(losses, "Transporte");

        model.addAttribute("currentPage", "losses");
        model.addAttribute("losses", losses);
        model.addAttribute("workOrders", workOrderService.listAll());
        model.addAttribute("newLoss", new Loss());

        model.addAttribute("totalLoss", totalLoss);
        model.addAttribute("pctRevenue", pctRevenue);
        model.addAttribute("pctCosts", pctCosts);

        model.addAttribute("valQuebra", quebra);
        model.addAttribute("valCorte", corte);
        model.addAttribute("valRetrabalho", retrabalho);
        model.addAttribute("valTransporte", transporte);

        return "losses";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("newLoss") Loss loss) {
        lossRepo.save(loss);
        return "redirect:/losses";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable UUID id) {
        lossRepo.deleteById(id);
        return "redirect:/losses";
    }

    private BigDecimal sumByType(List<Loss> losses, String type) {
        return losses.stream()
                .filter(l -> type.equals(l.getType()))
                .map(Loss::getFinancialImpact)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}