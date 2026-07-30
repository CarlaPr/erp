package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, UUID> {
    Optional<ExpenseCategory> findByCode(String code);
    java.util.List<ExpenseCategory> findByActiveTrueOrderByDisplayOrderAsc();
    java.util.List<ExpenseCategory> findAllByOrderByDisplayOrderAsc();
}
