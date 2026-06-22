package com.alfatahi.erp.repository;
import com.alfatahi.erp.entity.AccountsPayable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface AccountsPayableRepository extends JpaRepository<AccountsPayable, UUID> {
    List<AccountsPayable> findAllByOrderByDueDateAsc();
}