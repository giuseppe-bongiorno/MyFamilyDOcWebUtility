package it.myfamilydoc.webutility.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity per la tabella auth_schema.audit_log.
 * Registra tutte le operazioni admin con tracciabilità completa.
 */
@Entity
@Table(name = "audit_log", schema = "auth_schema")
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action")
    private String action;

    @Column(name = "username")
    private String username;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private String entityId;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "correlation_id", length = 36)
    private String correlationId;

    @Column(name = "ip_hash", length = 64)
    private String ipHash;

    @Column(name = "integrity_hash", length = 64)
    private String integrityHash;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public AuditLogEntity() {}

    // ── Getters & Setters ────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getIpHash() { return ipHash; }
    public void setIpHash(String ipHash) { this.ipHash = ipHash; }

    public String getIntegrityHash() { return integrityHash; }
    public void setIntegrityHash(String integrityHash) { this.integrityHash = integrityHash; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}