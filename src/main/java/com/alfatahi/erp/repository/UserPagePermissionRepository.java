package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.UserPagePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface UserPagePermissionRepository extends JpaRepository<UserPagePermission, UUID> {
    List<UserPagePermission> findByUserId(UUID userId);
}
