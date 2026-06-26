package com.alfatahi.erp.repository;
import com.alfatahi.erp.entity.AccountsReceivable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
public interface AccountsReceivableRepository extends JpaRepository<AccountsReceivable, UUID> {
    List<AccountsReceivable> findAllByOrderByDueDateAsc();

    @Query("SELECT SUM(a.receivedAmount) FROM AccountsReceivable a WHERE a.status IN ('received', 'partial') AND MONTH(a.dueDate) = :month AND YEAR(a.dueDate) = :year")
    BigDecimal sumReceivedByMonthAndYear(@Param("month") int month, @Param("year") int year);

}