package com.alfatahi.erp.repository;
import com.alfatahi.erp.entity.AccountsPayable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AccountsPayableRepository extends JpaRepository<AccountsPayable, UUID> {

    List<AccountsPayable> findAllByOrderByDueDateAsc();

    List<AccountsPayable> findByStatusNotOrderByDueDateAsc(String status);

    List<AccountsPayable> findByRecurrenceIdOrderByDueDateAsc(UUID recurrenceId);

    List<AccountsPayable> findByRecurrenceIdAndDueDateGreaterThanEqualOrderByDueDateAsc(UUID recurrenceId, LocalDate fromDueDate);

    @Query("SELECT SUM(a.totalAmount) FROM AccountsPayable a "
            + "WHERE a.category = :category "
            + "AND a.dueDate >= :inicio AND a.dueDate < :fim")
    BigDecimal sumPayablesByCategoryAndMonthAndYear(
            @Param("category") String category,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim);


    @Query("SELECT SUM(a.totalAmount) FROM AccountsPayable a " +
            "WHERE UPPER(a.expenseType) = 'VARIAVEL' " +
            "AND a.financialExpense = false " +
            "AND a.status != 'cancelled' " +
            "AND a.dueDate >= :inicio AND a.dueDate < :fim")
    BigDecimal sumCmvByMonthAndYear(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);


    @Query("SELECT SUM(a.totalAmount) FROM AccountsPayable a " +
            "WHERE UPPER(a.expenseType) != 'VARIAVEL' " +
            "AND a.financialExpense = false " +
            "AND a.status != 'cancelled' " +
            "AND a.dueDate >= :inicio AND a.dueDate < :fim")
    BigDecimal sumDespesasFixasByMonthAndYear(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("SELECT COALESCE(SUM(a.totalAmount), 0) FROM AccountsPayable a " +
            "WHERE a.financialExpense = true " +
            "AND a.status != 'cancelled' " +
            "AND a.dueDate >= :inicio AND a.dueDate < :fim")
    BigDecimal sumDespesasFinanceirasByMonthAndYear(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("SELECT COALESCE(SUM(p.paidAmount), 0) FROM AccountsPayable p WHERE p.status IN ('paid', 'partial')")
    BigDecimal sumTotalPayables();

    @Query("SELECT COALESCE(SUM(p.paidAmount), 0) FROM AccountsPayable p " +
            "WHERE p.financialExpense = true AND p.status IN ('paid', 'partial') " +
            "AND p.paymentDate >= :inicio AND p.paymentDate < :fim")
    BigDecimal sumDespesasFinanceirasRecebidas(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("SELECT COALESCE(SUM(p.paidAmount), 0) FROM AccountsPayable p " +
            "WHERE p.status IN ('paid', 'partial') " +
            "AND p.paymentDate >= :inicio AND p.paymentDate < :fim")
    BigDecimal sumSaidasRealByPeriod(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);


    @Query("SELECT COALESCE(SUM(p.totalAmount), 0) FROM AccountsPayable p " +
            "WHERE UPPER(p.category) = 'FIXA' AND p.status != 'cancelled' " +
            "AND p.dueDate >= :inicio AND p.dueDate < :fim")
    BigDecimal sumFixedTotalByMonth(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("SELECT COALESCE(SUM(p.paidAmount), 0) FROM AccountsPayable p " +
            "WHERE UPPER(p.category) = 'FIXA' AND p.status IN ('paid', 'partial') " +
            "AND p.dueDate >= :inicio AND p.dueDate < :fim")
    BigDecimal sumFixedPaidByMonth(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("SELECT COALESCE(SUM(p.totalAmount - p.paidAmount), 0) FROM AccountsPayable p " +
            "WHERE UPPER(p.category) = 'FIXA' AND p.status IN ('pending', 'partial') " +
            "AND p.dueDate >= :inicio AND p.dueDate < :fim")
    BigDecimal sumFixedPendingByMonth(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("SELECT p FROM AccountsPayable p WHERE UPPER(p.category) = 'FIXA' " +
            "AND p.status IN ('pending', 'partial') AND p.dueDate >= :hoje " +
            "ORDER BY p.dueDate ASC")
    List<AccountsPayable> findUpcomingFixedDue(@Param("hoje") LocalDate hoje);

    List<AccountsPayable> findByCategoryIgnoreCaseOrderByDueDateAsc(String category);
}
