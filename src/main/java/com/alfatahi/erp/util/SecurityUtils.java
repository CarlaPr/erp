package com.alfatahi.erp.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;


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


    public static boolean isTecnico() {
        Set<String> roles = currentRoles();
        return roles.contains("TECNICO") && !roles.contains("GESTAO") && !roles.contains("VENDAS");
    }
}
