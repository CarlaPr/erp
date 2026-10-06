package com.alfatahi.erp.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "user_page_permissions", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "page_key"}))
public class UserPagePermission {
    @Id
    private UUID id = UUID.randomUUID();
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "page_key", nullable = false, length = 64)
    private String pageKey;
    @Column(name = "can_view", nullable = false)
    private boolean canView;
    @Column(name = "can_edit", nullable = false)
    private boolean canEdit;
    @Column(name = "can_delete", nullable = false)
    private boolean canDelete;

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getPageKey() { return pageKey; }
    public void setPageKey(String pageKey) { this.pageKey = pageKey; }
    public boolean isCanView() { return canView; }
    public void setCanView(boolean canView) { this.canView = canView; }
    public boolean isCanEdit() { return canEdit; }
    public void setCanEdit(boolean canEdit) { this.canEdit = canEdit; }
    public boolean isCanDelete() { return canDelete; }
    public void setCanDelete(boolean canDelete) { this.canDelete = canDelete; }
}
