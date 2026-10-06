package com.alfatahi.erp.service;

import com.alfatahi.erp.dto.UserPermissionsForm;
import com.alfatahi.erp.entity.AppUser;
import com.alfatahi.erp.entity.UserPagePermission;
import com.alfatahi.erp.repository.AppUserRepository;
import com.alfatahi.erp.repository.UserPagePermissionRepository;
import com.alfatahi.erp.security.PageAccessService;
import com.alfatahi.erp.security.PageModule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserPermissionsService {
    private final AppUserRepository users;
    private final UserPagePermissionRepository permissions;
    private final PageAccessService access;

    public UserPermissionsService(AppUserRepository users, UserPagePermissionRepository permissions, PageAccessService access) {
        this.users = users;
        this.permissions = permissions;
        this.access = access;
    }

    public UserPermissionsForm formFor(AppUser user) {
        Map<String, UserPagePermission> existing = permissions.findByUserId(user.getId()).stream()
                .collect(Collectors.toMap(UserPagePermission::getPageKey, permission -> permission));
        UserPermissionsForm form = new UserPermissionsForm();
        for (PageModule page : PageModule.values()) {
            UserPermissionsForm.Row row = new UserPermissionsForm.Row();
            row.setPageKey(page.getKey());
            UserPagePermission custom = existing.get(page.getKey());
            PageAccessService.Access defaults = access.defaults(page, user.getRole());
            row.setMode(custom == null ? "default" : "custom");
            row.setView(custom == null ? defaults.canView() : custom.isCanView());
            row.setEdit(custom == null ? defaults.canEdit() : custom.isCanEdit());
            row.setDelete(custom == null ? defaults.canDelete() : custom.isCanDelete());
            form.getPages().add(row);
        }
        return form;
    }

    @Transactional
    public void save(UUID userId, UserPermissionsForm form) {
        AppUser user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        if ("GESTAO".equals(user.getRole())) {
            throw new IllegalArgumentException("A role Gestão mantém acesso completo e administra as permissões da equipe.");
        }
        if (form.getPages() == null || form.getPages().size() != PageModule.values().length) {
            throw new IllegalArgumentException("A lista de páginas está incompleta. Atualize a tela e tente novamente.");
        }
        Set<String> seen = new HashSet<>();
        for (UserPermissionsForm.Row row : form.getPages()) {
            if (row == null) throw new IllegalArgumentException("Configuração de página inválida.");
            PageModule page = PageModule.fromKey(row.getPageKey())
                    .orElseThrow(() -> new IllegalArgumentException("Página inválida."));
            if (!seen.add(row.getPageKey()) || !("default".equals(row.getMode()) || "custom".equals(row.getMode()))) {
                throw new IllegalArgumentException("Configuração de página inválida.");
            }
            if ("custom".equals(row.getMode()) && !row.isView() && (row.isEdit() || row.isDelete())) {
                throw new IllegalArgumentException("Ative Visualizar antes de permitir editar ou excluir em " + page.getLabel() + ".");
            }
            if ("custom".equals(row.getMode()) && !page.isWritable() && (row.isEdit() || row.isDelete())) {
                throw new IllegalArgumentException(page.getLabel() + " permite apenas visualização.");
            }
        }
        Map<String, UserPagePermission> existing = permissions.findByUserId(userId).stream()
                .collect(Collectors.toMap(UserPagePermission::getPageKey, permission -> permission));
        List<UserPagePermission> saved = new ArrayList<>();
        for (UserPermissionsForm.Row row : form.getPages()) {
            if ("default".equals(row.getMode())) continue;
            UserPagePermission permission = existing.remove(row.getPageKey());
            if (permission == null) permission = new UserPagePermission();
            permission.setUserId(userId);
            permission.setPageKey(row.getPageKey());
            permission.setCanView(row.isView());
            permission.setCanEdit(row.isEdit());
            permission.setCanDelete(row.isDelete());
            saved.add(permission);
        }
        permissions.deleteAll(List.copyOf(existing.values()));
        permissions.saveAll(saved);
    }
}
