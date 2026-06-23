package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.QuoteItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface QuoteItemRepository extends JpaRepository<QuoteItem, UUID> {
}