package com.alfatahi.erp.repository;

import com.alfatahi.erp.entity.MaterialPriceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface MaterialPriceItemRepository extends JpaRepository<MaterialPriceItem, UUID> {
    List<MaterialPriceItem> findByCategoryAndActiveTrueOrderByNameAsc(MaterialPriceItem.Category category);
    List<MaterialPriceItem> findAllByOrderByCategoryAscNameAsc();

    List<MaterialPriceItem> findByCategoryAndActiveTrueAndGlassTypeAndColorAndFinishAndThickness(
            MaterialPriceItem.Category category, String glassType, String color, String finish, java.math.BigDecimal thickness);
}
