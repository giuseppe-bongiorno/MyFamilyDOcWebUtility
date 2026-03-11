package it.myfamilydoc.webutility.dto;

/**
 * DTO per gli alert sanitari (pressione/glicemia fuori norma).
 */
public class HealthAlertDto {

    private Long id;
    private String severity;   // "critical", "warning", "info"
    private String userName;
    private String message;
    private String timestamp;

    public HealthAlertDto() {}

    public HealthAlertDto(Long id, String severity, String userName, String message, String timestamp) {
        this.id = id;
        this.severity = severity;
        this.userName = userName;
        this.message = message;
        this.timestamp = timestamp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}