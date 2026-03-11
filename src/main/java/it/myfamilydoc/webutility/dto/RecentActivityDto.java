package it.myfamilydoc.webutility.dto;

/**
 * DTO per le attività recenti nella dashboard.
 */
public class RecentActivityDto {

    private Long id;
    private String type;        // "user_registration", "document_upload", "certificate_issued", "message", "alert"
    private String description;
    private String timestamp;

    public RecentActivityDto() {}

    public RecentActivityDto(Long id, String type, String description, String timestamp) {
        this.id = id;
        this.type = type;
        this.description = description;
        this.timestamp = timestamp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}