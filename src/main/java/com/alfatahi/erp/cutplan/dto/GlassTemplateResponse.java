package com.alfatahi.erp.cutplan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * GlassTemplateResponse
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlassTemplateResponse {

    private UUID id;
    private String name;
    private String serviceCategoryName;
    private String description;
    private String defaultGlassType;
    private BigDecimal defaultThickness;
    private String defaultColor;
    private List<String> applicableFinishings;
    private Boolean active;
}
