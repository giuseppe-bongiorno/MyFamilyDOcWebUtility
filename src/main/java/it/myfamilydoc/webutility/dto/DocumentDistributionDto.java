package it.myfamilydoc.webutility.dto;

/**
 * DTO per la distribuzione documenti (pie chart).
 */
public class DocumentDistributionDto {

    private String type;
    private long count;

    public DocumentDistributionDto() {}

    public DocumentDistributionDto(String type, long count) {
        this.type = type;
        this.count = count;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
}