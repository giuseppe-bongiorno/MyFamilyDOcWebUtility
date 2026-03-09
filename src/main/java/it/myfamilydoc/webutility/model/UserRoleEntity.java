package it.myfamilydoc.webutility.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity per la tabella auth_schema.user_roles.
 */
@Entity
@Table(name = "user_roles", schema = "auth_schema")
public class UserRoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    public UserRoleEntity() {}

    public UserRoleEntity(Long userId, Long roleId) {
        this.userId = userId;
        this.roleId = roleId;
        this.assignedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }

    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }

    @PrePersist
    protected void onCreate() {
        if (this.assignedAt == null) {
            this.assignedAt = LocalDateTime.now();
        }
    }
}