package com.alfatahi.erp.cutplan.mapper;

import com.alfatahi.erp.cutplan.dto.CostTableResponse;
import com.alfatahi.erp.cutplan.entity.CostTable;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for CostTable entity -> CostTableResponse DTO.
 * MapStruct is not in pom.xml, so conversion is done manually.
 */
@Component
public class CostTableMapper {

    public CostTableResponse toResponse(CostTable c) {
        if (c == null) return null;
        return CostTableResponse.builder()
                .id(c.getId())
                .category(c.getCategory())
                .itemType(c.getItemType())
                .description(c.getDescription())
                .unitPrice(c.getUnitPrice())
                .unit(c.getUnit())
                .supplierName(c.getSupplier() != null ? c.getSupplier().getName() : null)
                .supplierId(c.getSupplier() != null ? c.getSupplier().getId() : null)
                .effectiveFrom(c.getEffectiveFrom() != null ? c.getEffectiveFrom().toString() : null)
                .effectiveTo(c.getEffectiveTo() != null ? c.getEffectiveTo().toString() : null)
                .active(c.getActive())
                .remarks(c.getRemarks())
                .build();
    }
}
