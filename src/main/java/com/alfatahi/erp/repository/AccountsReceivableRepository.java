package com.alfatahi.erp.repository;
import com.alfatahi.erp.entity.AccountsReceivable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface AccountsReceivableRepository extends JpaRepository<AccountsReceivable, UUID> {
    List<AccountsReceivable> findAllByOrderByDueDateAsc();
}