package com.alfatahi.erp.security;

import java.util.Set;

public enum PageAction {
    VIEW, EDIT, DELETE;

    private static final Set<String> DELETE_SEGMENTS = Set.of(
            "delete", "delete-single", "delete-future", "delete-series", "excluir", "remove",
            "cancel", "cancel-future", "inativar", "reopen-last", "reset-signature");
    private static final Set<String> EDIT_SEGMENTS = Set.of("new", "edit", "editar", "edit-data");

    public static PageAction forRequest(String method, String path) {
        if ("DELETE".equals(method)) return DELETE;
        for (String segment : path.split("/")) {
            if (DELETE_SEGMENTS.contains(segment)) return DELETE;
        }
        if ("GET".equals(method) || "HEAD".equals(method)) {
            for (String segment : path.split("/")) {
                if (EDIT_SEGMENTS.contains(segment)) return EDIT;
            }
            return VIEW;
        }
        return EDIT;
    }
}
