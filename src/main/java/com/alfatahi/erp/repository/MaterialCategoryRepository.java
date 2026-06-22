package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.MaterialCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface MaterialCategoryRepository extends JpaRepository<MaterialCategory, UUID> {}