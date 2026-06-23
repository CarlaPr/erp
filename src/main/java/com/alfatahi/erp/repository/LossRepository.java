package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.Loss;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LossRepository extends JpaRepository<Loss, UUID> {
    List<Loss> findAllByOrderByOccurrenceDateDesc();
}