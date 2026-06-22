package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, UUID> {}