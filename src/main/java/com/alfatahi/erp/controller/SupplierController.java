package com.alfatahi.erp.controller;

import com.alfatahi.erp.entity.Supplier;
import com.alfatahi.erp.service.SupplierService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("currentPage", "suppliers");
        model.addAttribute("suppliers", supplierService.listAllActive());
        model.addAttribute("newSupplier", new Supplier());
        return "suppliers";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("newSupplier") Supplier supplier) {
        supplierService.save(supplier);
        return "redirect:/suppliers";
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") UUID id, Model model) {
        model.addAttribute("currentPage", "suppliers");
        model.addAttribute("suppliers", supplierService.listAllActive());
        model.addAttribute("newSupplier", supplierService.findById(id));
        model.addAttribute("isEditing", true);
        return "suppliers";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable("id") UUID id) {
        supplierService.delete(id);
        return "redirect:/suppliers";
    }
}