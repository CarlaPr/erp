package com.alfatahi.erp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "work_order_checklists")
public class WorkOrderChecklist {
    @Id
    @Column(name = "work_order_id")
    private UUID workOrderId;

    protected WorkOrderChecklist() {}

    public WorkOrderChecklist(UUID workOrderId) {
        this.workOrderId = workOrderId;
    }
}
