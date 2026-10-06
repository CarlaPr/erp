package com.alfatahi.erp.controller;

import com.alfatahi.erp.dto.UserPermissionsForm;
import com.alfatahi.erp.service.UserPermissionsService;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.UUID;

@Controller
@RequestMapping("/settings/users")
public class UserPermissionsController {
    private final UserPermissionsService permissions;
    public UserPermissionsController(UserPermissionsService permissions) { this.permissions = permissions; }

    @PostMapping("/{id}/permissions")
    public String save(@PathVariable UUID id, @ModelAttribute("permissionForm") UserPermissionsForm form,
                       BindingResult binding, RedirectAttributes redirect) {
        try {
            if (binding.hasErrors()) throw new IllegalArgumentException("Permissões inválidas. Atualize a tela e tente novamente.");
            permissions.save(id, form);
            redirect.addFlashAttribute("permissionsSuccess", "Permissões atualizadas. A alteração já vale para as próximas ações do usuário.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("permissionsError", exception.getMessage());
        }
        redirect.addAttribute("permissionUser", id);
        return "redirect:/settings#pagePermissions";
    }
}
