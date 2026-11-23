package com.alfatahi.erp.dto;

import com.alfatahi.erp.entity.WorkOrderChecklistItem;
import com.alfatahi.erp.entity.WorkOrderExecutionNote;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record TechnicianWorkOrderView(Summary order, LocalDateTime createdAt, LocalTime installationTime,
                                      String technician, String team, String description, String notes,
                                      String installationNotes, String agendaNotes, String siteContact, String sitePhone,
                                      String accessInstructions, String mapUrl, String callUrl,
                                      String whatsappUrl, Customer customer, List<ContractedService> services,
                                      List<WorkOrderChecklistItem> items, List<WorkOrderExecutionNote> executionNotes) {
    public record Summary(UUID id, String number, String customerName, String address, String phone,
                          List<String> services, LocalDate installationDate, String status, boolean completed) {}

    public record Customer(String name, String document, String phone, String email, String address,
                           String city, String notes) {}

    public record ContractedService(String name, String category, String description, BigDecimal quantity,
                          List<Measurement> measurements, String fieldMeasurements, String fieldNotes) {}

    public record Measurement(BigDecimal width, BigDecimal height, BigDecimal quantity) {}

    public long completedItems() {
        return items.stream().filter(WorkOrderChecklistItem::isCompleted).count();
    }

    public double progress() {
        return items.isEmpty() ? 0 : completedItems() * 100.0 / items.size();
    }
}
