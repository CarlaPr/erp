package com.alfatahi.erp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


public record ReceiptSaveRequest(
        UUID id,
        UUID profileId,
        UUID clientId,
        UUID workOrderId,
        LocalDate issueDate,
        LocalDate receivedDate,
        BigDecimal discount,
        String paymentTerms,
        String warranty,
        String description,
        List<Item> items,
        List<Photo> photos) {

    public record Item(String description, BigDecimal quantity, BigDecimal unitPrice) { }

    public record Photo(UUID id, String dataUri) { }
}
