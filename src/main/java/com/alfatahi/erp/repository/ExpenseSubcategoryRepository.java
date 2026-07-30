package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.ExpenseSubcategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ExpenseSubcategoryRepository extends JpaRepository<ExpenseSubcategory, UUID> {

    List<ExpenseSubcategory> findAllByOrderByDisplayOrderAsc();

    @Query("SELECT s FROM ExpenseSubcategory s WHERE s.category.code = :categoryCode " +
            "AND s.active = true ORDER BY s.displayOrder ASC")
    List<ExpenseSubcategory> findByCategoryCode(@Param("categoryCode") String categoryCode);
}
