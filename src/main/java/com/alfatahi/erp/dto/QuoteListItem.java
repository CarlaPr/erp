package com.alfatahi.erp.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class QuoteListItem {
    private final UUID id;
    private final String number;
    private final String status;
    private final LocalDateTime dateCreated;
    private final String clientName;
    private final UUID workOrderId;
    private final String workOrderNumber;
    private final String publicToken;
    private final boolean clientSigned;

    public QuoteListItem(UUID id, String number, String status, LocalDateTime dateCreated,
                         String clientName, UUID workOrderId, String workOrderNumber,
                         String publicToken, Boolean clientSigned) {
        this.id = id;
        this.number = number;
        this.status = status;
        this.dateCreated = dateCreated;
        this.clientName = clientName;
        this.workOrderId = workOrderId;
        this.workOrderNumber = workOrderNumber;
        this.publicToken = publicToken;
        this.clientSigned = Boolean.TRUE.equals(clientSigned);
    }

    public UUID getId() { return id; }
    public String getNumber() { return number; }
    public String getStatus() { return status; }
    public LocalDateTime getDateCreated() { return dateCreated; }
    public String getClientName() { return clientName; }
    public UUID getWorkOrderId() { return workOrderId; }
    public String getWorkOrderNumber() { return workOrderNumber; }
    public String getPublicToken() { return publicToken; }
    public boolean isClientSigned() { return clientSigned; }
}
