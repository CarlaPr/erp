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

    @Query("SELECT SUM(a.totalAmount) FROM AccountsPayable a "
            + "WHERE a.category = :category "
            + "AND a.dueDate >= :inicio AND a.dueDate < :fim")
    BigDecimal sumPayablesByCategoryAndMonthAndYear(
            @Param("category") String category,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim);

    /** CMV = despesas de categoria 'variable' que NÃO são despesa financeira */
    @Query("SELECT SUM(a.totalAmount) FROM AccountsPayable a " +
            "WHERE LOWER(a.category) = 'variable' " +
            "AND a.financialExpense = false " +
            "AND a.status != 'cancelled' " +
            "AND a.dueDate >= :inicio AND a.dueDate < :fim")
    BigDecimal sumCmvByMonthAndYear(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    /** Despesas Fixas = tudo que NÃO é variable e NÃO é despesa financeira */
    @Query("SELECT SUM(a.totalAmount) FROM AccountsPayable a " +
            "WHERE LOWER(a.category) != 'variable' " +
            "AND a.financialExpense = false " +
            "AND a.status != 'cancelled' " +
            "AND a.dueDate >= :inicio AND a.dueDate < :fim")
    BigDecimal sumDespesasFixasByMonthAndYear(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    /** Despesas Financeiras (taxa de cartão, tarifas bancárias) — linha própria no DRE */
    @Query("SELECT COALESCE(SUM(a.totalAmount), 0) FROM AccountsPayable a " +
            "WHERE a.financialExpense = true " +
            "AND a.status != 'cancelled' " +
            "AND a.dueDate >= :inicio AND a.dueDate < :fim")
    BigDecimal sumDespesasFinanceirasByMonthAndYear(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("SELECT COALESCE(SUM(p.paidAmount), 0) FROM AccountsPayable p WHERE p.status IN ('paid', 'partial')")
    BigDecimal sumTotalPayables();

    /** Total de despesas financeiras já pagas — para o Livro Caixa e Dashboard */
    @Query("SELECT COALESCE(SUM(p.paidAmount), 0) FROM AccountsPayable p " +
            "WHERE p.financialExpense = true AND p.status IN ('paid', 'partial') " +
            "AND p.paymentDate >= :inicio AND p.paymentDate < :fim")
    BigDecimal sumDespesasFinanceirasRecebidas(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    /** Saídas reais (pagas) no período — para o Livro Caixa */
    @Query("SELECT COALESCE(SUM(p.paidAmount), 0) FROM AccountsPayable p " +
            "WHERE p.status IN ('paid', 'partial') " +
            "AND p.paymentDate >= :inicio AND p.paymentDate < :fim")
    BigDecimal sumSaidasRealByPeriod(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);
}
