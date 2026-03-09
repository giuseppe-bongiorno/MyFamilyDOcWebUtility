package it.myfamilydoc.webutility.dto;

/**
 * Request DTO per la soft-delete di un utente.
 */
public class DeleteUserRequest {

    private String reason;

    public DeleteUserRequest() {}

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}