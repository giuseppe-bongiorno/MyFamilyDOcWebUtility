package it.myfamilydoc.webutility.dto;

import java.time.LocalDateTime;

/**
 * DTO per la risposta della lista utenti nella pagina User Management.
 * Mappa i campi richiesti dal frontend UserManagementPage.tsx
 */
public class UserManagementDto {

    private Long id;
    private String username;
    private String email;
    private String role;          // Ruolo principale: ADMIN, DEV, DOC, USER
    private boolean enabled;
    private boolean emailVerified;
    private String lastLoginAt;
    private String lastLoginIp;
    private String createdAt;
    private String deletedAt;

    public UserManagementDto() {}

    // ── Getters & Setters ────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public String getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(String lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public String getLastLoginIp() { return lastLoginIp; }
    public void setLastLoginIp(String lastLoginIp) { this.lastLoginIp = lastLoginIp; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getDeletedAt() { return deletedAt; }
    public void setDeletedAt(String deletedAt) { this.deletedAt = deletedAt; }
}