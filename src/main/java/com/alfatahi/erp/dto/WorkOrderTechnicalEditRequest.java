package com.alfatahi.erp.dto;

import java.util.List;
import java.util.UUID;

public record WorkOrderTechnicalEditRequest(
        String installationAddress,
        String siteContact,
        String sitePhone,
        String technicianName,
        String accessInstructions,
        String installationNotes,
        String conditionsObservations,
        String warrantyTerms,
        Boolean showMeasurements,
        List<ServiceItem> items
) {
    public record ServiceItem(UUID id, String technicalMeasurements, String technicalNotes) { }
}
