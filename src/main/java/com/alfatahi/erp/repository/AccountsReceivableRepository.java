package com.alfatahi.erp.repository;
import com.alfatahi.erp.entity.AccountsReceivable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AccountsReceivableRepository extends JpaRepository<AccountsReceivable, UUID> {

    List<AccountsReceivable> findAllByOrderByDueDateAsc();

    List<AccountsReceivable> findByStatusNotOrderByDueDateAsc(String status);

    @Query("SELECT SUM(COALESCE(a.receivedAmount,0) + COALESCE(a.feeAmount,0)) FROM AccountsReceivable a " +
            "WHERE a.paymentDate >= :inicio AND a.paymentDate < :fim")
    BigDecimal sumReceivedByMonthAndYear(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("SELECT SUM(a.totalAmount) FROM AccountsReceivable a WHERE a.status != 'cancelled'")
    BigDecimal sumTotalAmount();

    @Query("SELECT COALESCE(SUM(r.receivedAmount), 0) FROM AccountsReceivable r WHERE r.status IN ('received', 'partial')")
    BigDecimal sumTotalReceivables();
}
