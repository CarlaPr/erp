package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.ExpenseAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ExpenseAllocationRepository extends JpaRepository<ExpenseAllocation, UUID> {

    List<ExpenseAllocation> findByAccountsPayableId(UUID accountsPayableId);

    List<ExpenseAllocation> findByWorkOrderId(UUID workOrderId);

    @Query("SELECT ea FROM ExpenseAllocation ea " +
            "LEFT JOIN FETCH ea.workOrder " +
            "WHERE ea.accountsPayable.id IN :payableIds")
    List<ExpenseAllocation> findByAccountsPayableIdIn(@Param("payableIds") Collection<UUID> payableIds);
}
