package com.alfatahi.erp.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Set;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/keep-alive")
    @ResponseBody
    public ResponseEntity<Void> keepAlive(HttpServletRequest request) {
        request.getSession(true);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/login-success")
    public String loginSuccess(Authentication authentication) {
        Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());

        if (roles.contains("GESTAO")) {
            return "redirect:/dashboard";
        } else if (roles.contains("VENDAS")) {
            return "redirect:/commercial";
        } else if (roles.contains("TECNICO")) {
            return "redirect:/agenda";
        }
        return "redirect:/login";
    }

    @GetMapping("/acesso-negado")
    public String acessoNegado() {
        return "acesso-negado";
    }
}