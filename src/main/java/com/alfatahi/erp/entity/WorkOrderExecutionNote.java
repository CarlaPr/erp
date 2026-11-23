package com.alfatahi.erp.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "work_order_execution_notes")
public class WorkOrderExecutionNote {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, updatable = false)
    private UUID workOrderId;
    @Column(updatable = false)
    private UUID authorId;
    @Column(nullable = false, updatable = false)
    private String authorName;
    @Column(nullable = false, updatable = false, columnDefinition = "TEXT")
    private String body;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected WorkOrderExecutionNote() {}

    public WorkOrderExecutionNote(UUID workOrderId, AppUser author, String body) {
        this.workOrderId = workOrderId;
        this.authorId = author.getId();
        this.authorName = author.getUsername();
        this.body = body.strip();
    }

    public String getAuthorName() { return authorName; }
    public String getBody() { return body; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
