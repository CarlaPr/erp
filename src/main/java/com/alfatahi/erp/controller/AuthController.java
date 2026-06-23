package com.alfatahi.erp.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Set;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/login-success")
    public String loginSuccess(Authentication authentication) {
        Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());

        // REQUISITO: Redirecionamento automático com base na role
        if (roles.contains("ROLE_GESTAO")) {
            return "redirect:/dashboard";
        } else if (roles.contains("ROLE_VENDAS")) {
            return "redirect:/commercial"; // Dashboard comercial da próxima fase
        }
        return "redirect:/login";
    }
}