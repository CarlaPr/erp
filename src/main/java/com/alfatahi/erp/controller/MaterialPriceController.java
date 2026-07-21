package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.MaterialPriceItem;
import com.alfatahi.erp.repository.MaterialPriceItemRepository;
import com.alfatahi.erp.repository.SupplierRepository;
import com.alfatahi.erp.service.MaterialPriceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Catálogo de Insumos / Tabela de Preços de Compra.
 * Módulo administrativo que alimenta o painel de custos do Plano de Corte.
 */
@Controller
@RequestMapping("/price-catalog")
public class MaterialPriceController {

    private final MaterialPriceService materialPriceService;
    private final MaterialPriceItemRepository repository;
    private final SupplierRepository supplierRepository;

    public MaterialPriceController(MaterialPriceService materialPriceService, MaterialPriceItemRepository repository,
                                    SupplierRepository supplierRepository) {
        this.materialPriceService = materialPriceService;
        this.repository = repository;
        this.supplierRepository = supplierRepository;
    }

    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "sistema";
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("items", materialPriceService.listAll());
        model.addAttribute("suppliers", supplierRepository.findAll());
        model.addAttribute("categories", MaterialPriceItem.Category.values());
        model.addAttribute("units", MaterialPriceItem.Unit.values());
        model.addAttribute("currentPage", "price-catalog");
        return "price-catalog";
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<MaterialPriceItem> get(@PathVariable UUID id) {
        return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/save-ajax")
    @ResponseBody
    public ResponseEntity<MaterialPriceItem> save(@RequestBody Map<String, Object> body) {
        MaterialPriceItem item = new MaterialPriceItem();
        if (body.get("id") != null && !body.get("id").toString().isBlank()) item.setId(UUID.fromString(body.get("id").toString()));
        item.setCategory(MaterialPriceItem.Category.valueOf((String) body.get("category")));
        item.setName((String) body.get("name"));
        item.setManufacturer((String) body.get("manufacturer"));
        item.setGlassType((String) body.get("glassType"));
        item.setColor((String) body.get("color"));
        item.setFinish((String) body.get("finish"));
        item.setAluminumLine((String) body.get("aluminumLine"));
        item.setAluminumProfile((String) body.get("aluminumProfile"));
        item.setHardwareCategory((String) body.get("hardwareCategory"));
        item.setCode((String) body.get("code"));
        if (body.get("thickness") != null && !body.get("thickness").toString().isBlank())
            item.setThickness(new java.math.BigDecimal(body.get("thickness").toString()));
        item.setUnit(MaterialPriceItem.Unit.valueOf((String) body.get("unit")));
        item.setPrice(new java.math.BigDecimal(body.get("price").toString()));
        if (body.get("minPrice") != null && !body.get("minPrice").toString().isBlank())
            item.setMinPrice(new java.math.BigDecimal(body.get("minPrice").toString()));
        if (body.get("supplierId") != null && !body.get("supplierId").toString().isBlank()) {
            supplierRepository.findById(UUID.fromString(body.get("supplierId").toString())).ifPresent(item::setSupplier);
        }
        item.setNotes((String) body.get("notes"));

        String reason = (String) body.getOrDefault("reason", "Atualização manual de preço");
        return ResponseEntity.ok(materialPriceService.save(item, currentUser(), reason));
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        materialPriceService.delete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/history")
    @ResponseBody
    public ResponseEntity<?> history(@PathVariable UUID id) {
        return ResponseEntity.ok(materialPriceService.history(id));
    }

    @PostMapping("/import")
    @ResponseBody
    public ResponseEntity<?> importExcel(@RequestParam("file") MultipartFile file) {
        try {
            MaterialPriceService.ImportResult result = materialPriceService.importFromExcel(file, currentUser());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
