package it.myfamilydoc.webutility.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO per il cambio ruolo utente.
 */
public class ChangeRoleRequest {

    @NotBlank(message = "Il ruolo è obbligatorio")
    @Pattern(regexp = "^(ADMIN|DEV|DOC|USER)$", message = "Ruolo non valido. Valori ammessi: ADMIN, DEV, DOC, USER")
    private String role;

    public ChangeRoleRequest() {}

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}