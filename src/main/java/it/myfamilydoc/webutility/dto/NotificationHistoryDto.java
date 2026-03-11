package it.myfamilydoc.webutility.dto;

/**
 * DTO per la cronologia notifiche push dalla tabella push_notification_log.
 */
public class NotificationHistoryDto {

    private Long id;
    private String user;       // username del destinatario
    private String title;
    private String body;
    private String status;     // SENT, DELIVERED, READ, FAILED
    private String timestamp;  // created_at
    private int devices;       // conteggio dispositivi (1 per riga)

    public NotificationHistoryDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public int getDevices() { return devices; }
    public void setDevices(int devices) { this.devices = devices; }
}