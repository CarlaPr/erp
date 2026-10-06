package com.alfatahi.erp.controller;

import com.alfatahi.erp.security.PageAccessService;
import com.alfatahi.erp.security.PageModule;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class PageAccessAdvice {
    private final PageAccessService access;
    public PageAccessAdvice(PageAccessService access) { this.access = access; }

    @ModelAttribute
    public void permissions(HttpServletRequest request, Model model) {
        PageModule.fromPath(PageAccessService.requestPath(request)).ifPresent(page -> {
            model.addAttribute("pagePermission", access.access(page.getKey()));
            model.addAttribute("permissionPageKey", page.getKey());
            model.addAttribute("pageHasWriteActions", page.isWritable());
        });
    }
}
