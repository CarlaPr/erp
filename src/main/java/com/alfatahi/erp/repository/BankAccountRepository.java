package com.alfatahi.erp.repository;
import com.alfatahi.erp.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface BankAccountRepository extends JpaRepository<BankAccount, UUID> {}