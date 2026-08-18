package com.alfatahi.erp.dto;

import com.alfatahi.erp.planocorte.entity.CategoriaServico;
import com.alfatahi.erp.planocorte.entity.ReferenciaHorizontal;
import com.alfatahi.erp.planocorte.entity.ReferenciaVertical;
import com.alfatahi.erp.planocorte.entity.TipoElemento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record TechnicalVisitDetailsDto(
        UUID id, UUID clientId, String clientName, String clientAddress,
        UUID quoteId, String quoteNumber, LocalDate visitDate, LocalTime visitTime,
        String notes, String status, List<OpeningDto> openings, List<PhotoDto> photos) {

    public record OpeningDto(
            UUID id, CategoriaServico serviceCategory, String name, BigDecimal widthMm, BigDecimal heightMm,
            BigDecimal grossHeightLeftMm, BigDecimal grossHeightRightMm,
            BigDecimal grossWidthTopMm, BigDecimal grossWidthBottomMm,
            String notes, List<FeatureDto> features) { }

    public record FeatureDto(
            UUID id, TipoElemento type, String name,
            ReferenciaHorizontal referenceHorizontal, BigDecimal distanceHorizontalMm,
            ReferenciaVertical referenceVertical, BigDecimal distanceVerticalMm,
            BigDecimal diameterMm, BigDecimal widthMm, BigDecimal heightMm,
            BigDecimal depthMm, BigDecimal radiusMm, String corner, String notes) { }

    public record PhotoDto(UUID id, String fileName, String contentType, Long fileSize, String caption) { }
}
