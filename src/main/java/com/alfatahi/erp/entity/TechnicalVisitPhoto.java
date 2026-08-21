package com.alfatahi.erp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "technical_visit_photos")
public class TechnicalVisitPhoto {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "technical_visit_id", nullable = false)
    @JsonIgnore
    private TechnicalVisit technicalVisit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opening_id")
    @JsonIgnore
    private TechnicalVisitOpening opening;

    @Column(name = "file_name", nullable = false)
    private String fileName;
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;
    @Column(name = "file_size", nullable = false)
    private Long fileSize;
    @Column(length = 180)
    private String caption;
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false)
    private byte[] content;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }

    public UUID getId() { return id; }
    public TechnicalVisit getTechnicalVisit() { return technicalVisit; }
    public void setTechnicalVisit(TechnicalVisit value) { this.technicalVisit = value; }
    public TechnicalVisitOpening getOpening() { return opening; }
    public void setOpening(TechnicalVisitOpening value) { this.opening = value; }
    public String getFileName() { return fileName; }
    public void setFileName(String value) { this.fileName = value; }
    public String getContentType() { return contentType; }
    public void setContentType(String value) { this.contentType = value; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long value) { this.fileSize = value; }
    public String getCaption() { return caption; }
    public void setCaption(String value) { this.caption = value; }
    public byte[] getContent() { return content; }
    public void setContent(byte[] value) { this.content = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
