package com.alfatahi.erp.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

/**
 * Utilitário central para obter o usuário autenticado atual e suas roles,
 * usado para restringir o que o perfil TECNICO pode ver/fazer na Agenda Comercial.
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    public static Authentication currentAuth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static String currentUsername() {
        Authentication auth = currentAuth();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "sistema";
    }

    public static Set<String> currentRoles() {
        Authentication auth = currentAuth();
        if (auth == null) return Set.of();
        return AuthorityUtils.authorityListToSet(auth.getAuthorities());
    }

    /**
     * TÉCNICO "puro": usuário com a role TECNICO e sem GESTAO/VENDAS.
     * Usuários com múltiplas roles (ex.: GESTAO+TECNICO) mantêm acesso completo.
     */
    public static boolean isTecnico() {
        Set<String> roles = currentRoles();
        return roles.contains("TECNICO") && !roles.contains("GESTAO") && !roles.contains("VENDAS");
    }
}
