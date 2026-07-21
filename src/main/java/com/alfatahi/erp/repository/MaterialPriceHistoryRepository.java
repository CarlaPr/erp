package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.MaterialPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface MaterialPriceHistoryRepository extends JpaRepository<MaterialPriceHistory, UUID> {
    List<MaterialPriceHistory> findByPriceItemIdOrderByChangedAtDesc(UUID priceItemId);
}
