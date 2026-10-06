package com.alfatahi.erp.security;

import com.alfatahi.erp.entity.UserPagePermission;
import com.alfatahi.erp.repository.AppUserRepository;
import com.alfatahi.erp.repository.UserPagePermissionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UrlPathHelper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service("pageAccess")
public class PageAccessService {
    private static final String SNAPSHOT_KEY = PageAccessService.class.getName() + ".snapshot";
    private static final Access MANAGEMENT_ACCESS = new Access(true, true, true, false);
    private final AppUserRepository users;
    private final UserPagePermissionRepository permissions;

    public PageAccessService(AppUserRepository users, UserPagePermissionRepository permissions) {
        this.users = users;
        this.permissions = permissions;
    }

    public static String requestPath(HttpServletRequest request) {
        // Match the same decoded application path used by MVC, including encoded URL characters.
        return UrlPathHelper.defaultInstance.getPathWithinApplication(request);
    }

    public static boolean isManagementRequest(HttpServletRequest request) {
        String path = requestPath(request);
        return path.equals("/settings") || path.startsWith("/settings/")
                || path.equals("/admin/users") || path.startsWith("/admin/users/");
    }

    public record Access(boolean canView, boolean canEdit, boolean canDelete, boolean customized) {
        public boolean allows(PageAction action) {
            return canView && switch (action) {
                case VIEW -> true;
                case EDIT -> canEdit;
                case DELETE -> canDelete;
            };
        }
    }

    private record Snapshot(String username, String role, Map<String, UserPagePermission> permissions) {}

    private Snapshot snapshot(Authentication auth, HttpServletRequest request) {
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return new Snapshot("", "", Map.of());
        }
        if (request != null && request.getAttribute(SNAPSHOT_KEY) instanceof Snapshot cached
                && cached.username().equals(auth.getName())) return cached;
        Snapshot result = users.findByUsername(auth.getName()).map(user -> new Snapshot(
                user.getUsername(), user.getRole(), "GESTAO".equals(user.getRole()) ? Map.of() : permissions.findByUserId(user.getId()).stream()
                .collect(Collectors.toMap(UserPagePermission::getPageKey, permission -> permission))))
                .orElseGet(() -> new Snapshot(auth.getName(), "", Map.of()));
        // Cache only within this request. Active sessions see grants/revocations on their next request.
        if (request != null) request.setAttribute(SNAPSHOT_KEY, result);
        return result;
    }

    private HttpServletRequest currentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? attributes.getRequest() : null;
    }

    private Snapshot current() {
        return snapshot(SecurityContextHolder.getContext().getAuthentication(), currentRequest());
    }

    public boolean isManager() { return "GESTAO".equals(current().role()); }
    public boolean isManager(Authentication auth, HttpServletRequest request) {
        return "GESTAO".equals(snapshot(auth, request).role());
    }

    public Access defaults(PageModule page, String role) {
        if ("GESTAO".equals(role)) return MANAGEMENT_ACCESS;
        boolean view = page.allowedByRole(role);
        boolean write = view && page.isWritable();
        if (page == PageModule.AGENDA && "TECNICO".equals(role)) {
            return new Access(view, true, false, false); // Technician notes keep their existing access.
        }
        if (page == PageModule.TECHNICAL_VISITS) {
            return new Access(view, write, "GESTAO".equals(role) || "TECNICO".equals(role), false);
        }
        return new Access(view, write, write, false);
    }

    private Access access(PageModule page, Snapshot snapshot) {
        if ("GESTAO".equals(snapshot.role())) return MANAGEMENT_ACCESS;
        UserPagePermission permission = snapshot.permissions().get(page.getKey());
        if (permission == null) return defaults(page, snapshot.role());
        return new Access(permission.isCanView(), page.isWritable() && permission.isCanEdit(),
                page.isWritable() && permission.isCanDelete(), true);
    }

    public Access access(String key) {
        return PageModule.fromKey(key).map(page -> access(page, current()))
                .orElseGet(() -> new Access(false, false, false, false));
    }

    public boolean canView(String key) { return access(key).canView(); }
    public boolean canEdit(String key) { return access(key).allows(PageAction.EDIT); }
    public boolean canDelete(String key) { return access(key).allows(PageAction.DELETE); }
    public boolean canViewAny(String... keys) {
        for (String key : keys) if (canView(key)) return true;
        return false;
    }

    public boolean hasCustomAccess(String key) { return access(key).customized(); }
    public boolean canPlanVisits() { return authorizeCurrent("POST", "/technical-visits/create"); }
    public boolean canWorkVisits() { return authorizeCurrent("POST", "/technical-visits/visit/openings"); }

    private boolean authorizeCurrent(String method, String path) { return authorize(current(), method, path); }

    public boolean authorize(Authentication auth, HttpServletRequest request) {
        String path = requestPath(request);
        return authorize(snapshot(auth, request), request.getMethod(), path);
    }

    private boolean authorize(Snapshot snapshot, String method, String path) {
        PageModule page = PageModule.fromPath(path).orElse(null);
        if (page == null) return false;
        // The editor reads compatible glass without requiring access to catalog management.
        if (("GET".equals(method) || "HEAD".equals(method))
                && path.equals("/cut-plans/catalogo/vidros-compativeis")
                && access(PageModule.CUT_PLANS, snapshot).canView()) return true;
        PageAction action = PageAction.forRequest(method, path);
        Access access = access(page, snapshot);
        if (!access.allows(action)) return false;
        if (page == PageModule.AGENDA && !access.customized() && "TECNICO".equals(snapshot.role())
                && action == PageAction.EDIT && !path.endsWith("/technician-note")) return false;
        if (page == PageModule.TECHNICAL_VISITS && !access.customized() && action == PageAction.EDIT) {
            boolean field = path.matches("/technical-visits/[^/]+/(start|complete|openings|photos)");
            if (field && "VENDAS".equals(snapshot.role())) return false;
            if (!field && "TECNICO".equals(snapshot.role())) return false;
        }
        // Client changes embedded in another page must also respect the client's permissions.
        if ((path.equals("/quotes/add-client-ajax") || path.startsWith("/quotes/update-client-ajax/")
                || path.equals("/technical-visits/clients"))
                && !access(PageModule.CLIENTS, snapshot).allows(PageAction.EDIT)) return false;
        return true;
    }

    public List<PageModule> modules() { return List.of(PageModule.values()); }
}
