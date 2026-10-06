package com.alfatahi.erp.controller;

import com.alfatahi.erp.security.PageAccessService;
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
    private final PageAccessService pageAccess;

    public AuthController(PageAccessService pageAccess) { this.pageAccess = pageAccess; }

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

        if (pageAccess.isManager()) {
            return "redirect:/dashboard";
        } else if (roles.contains("VENDAS") && pageAccess.canView("commercial")) {
            return "redirect:/commercial";
        } else if (roles.contains("TECNICO") && pageAccess.canView("agenda")) {
            return "redirect:/agenda";
        }
        return pageAccess.modules().stream().filter(page -> pageAccess.canView(page.getKey())).findFirst()
                .map(page -> "redirect:" + page.getPath()).orElse("redirect:/acesso-negado");
    }

    @GetMapping("/acesso-negado")
    public String acessoNegado() {
        return "acesso-negado";
    }
}
