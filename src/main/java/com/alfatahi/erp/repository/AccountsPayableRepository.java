package com.alfatahi.erp.repository;
import com.alfatahi.erp.entity.AccountsPayable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
public interface AccountsPayableRepository extends JpaRepository<AccountsPayable, UUID> {
    List<AccountsPayable> findAllByOrderByDueDateAsc();

    @Query("SELECT SUM(a.totalAmount) FROM AccountsPayable a WHERE a.category = :category AND MONTH(a.dueDate) = :month AND YEAR(a.dueDate) = :year")
    BigDecimal sumPayablesByCategoryAndMonthAndYear(@Param("category") String category, @Param("month") int month, @Param("year") int year);

    @Query("SELECT SUM(a.totalAmount) FROM AccountsPayable a WHERE LOWER(a.category) = 'variable' AND MONTH(a.dueDate) = :month AND YEAR(a.dueDate) = :year")
    BigDecimal sumCmvByMonthAndYear(@Param("month") int month, @Param("year") int year);

    @Query("SELECT SUM(a.totalAmount) FROM AccountsPayable a WHERE LOWER(a.category) != 'variable' AND MONTH(a.dueDate) = :month AND YEAR(a.dueDate) = :year")
    BigDecimal sumDespesasFixasByMonthAndYear(@Param("month") int month, @Param("year") int year);

}