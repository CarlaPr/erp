package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.CutRuleSet;
import com.alfatahi.erp.repository.CutRuleSetRepository;
import com.alfatahi.erp.repository.ServiceCategoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Tela administrativa de Regras de Desconto/Folga técnica (parametrização
 * usada pelo Plano de Corte). Nenhum valor fica fixo no código: o
 * administrador cadastra, edita e desativa conjuntos de regras por
 * fabricante/linha de perfil aqui.
 */
@Controller
@RequestMapping("/cut-rule-sets")
public class CutRuleSetController {

    private final CutRuleSetRepository ruleSetRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;

    public CutRuleSetController(CutRuleSetRepository ruleSetRepository, ServiceCategoryRepository serviceCategoryRepository) {
        this.ruleSetRepository = ruleSetRepository;
        this.serviceCategoryRepository = serviceCategoryRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String index(Model model) {
        model.addAttribute("ruleSets", ruleSetRepository.findAll());
        model.addAttribute("categories", serviceCategoryRepository.findAll());
        model.addAttribute("currentPage", "cut-rule-sets");
        return "cut-rule-sets";
    }

    @GetMapping("/{id}")
    @ResponseBody
    @Transactional(readOnly = true)
    public ResponseEntity<CutRuleSet> get(@PathVariable UUID id) {
        return ruleSetRepository.findById(id).map(rs -> {
            org.hibernate.Hibernate.initialize(rs.getParameters());
            return ResponseEntity.ok(rs);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/save-ajax")
    @ResponseBody
    @Transactional
    public ResponseEntity<CutRuleSet> save(@RequestBody CutRuleSet incoming) {
        CutRuleSet target = incoming.getId() != null
                ? ruleSetRepository.findById(incoming.getId()).orElse(new CutRuleSet())
                : new CutRuleSet();
        target.setName(incoming.getName());
        target.setDescription(incoming.getDescription());
        target.setGlassType(incoming.getGlassType());
        target.setActive(incoming.getActive());
        target.setServiceCategory(incoming.getServiceCategory());
        target.setParameters(incoming.getParameters());
        if (target.getParameters() != null) {
            target.getParameters().forEach(p -> p.setRuleSet(target));
        }
        return ResponseEntity.ok(ruleSetRepository.save(target));
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        ruleSetRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
