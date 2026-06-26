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

    @Query("SELECT SUM(a.totalAmount) FROM AccountsPayable a WHERE a.category = :category AND MONTH(a.dueDate) = :month AND YEAR(a.dueDate) = :year")
    BigDecimal sumPayablesByCategoryAndMonthAndYear(@Param("category") String category, @Param("month") int month, @Param("year") int year);

    @Query("SELECT SUM(a.totalAmount) FROM AccountsPayable a " +
            "WHERE LOWER(a.category) = 'variable' " +
            "AND a.dueDate >= :inicio AND a.dueDate < :fim")
    BigDecimal sumCmvByMonthAndYear(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("SELECT SUM(a.totalAmount) FROM AccountsPayable a " +
            "WHERE LOWER(a.category) != 'variable' " +
            "AND a.dueDate >= :inicio AND a.dueDate < :fim")
    BigDecimal sumDespesasFixasByMonthAndYear(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);
}
