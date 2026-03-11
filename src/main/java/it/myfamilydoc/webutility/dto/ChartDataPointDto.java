package it.myfamilydoc.webutility.dto;

/**
 * DTO per i dati del grafico attività (ultimi 30 giorni).
 */
public class ChartDataPointDto {

    private String date;    // formato ISO: "2025-03-01"
    private long documents;
    private long users;
    private long messages;

    public ChartDataPointDto() {}

    public ChartDataPointDto(String date, long documents, long users, long messages) {
        this.date = date;
        this.documents = documents;
        this.users = users;
        this.messages = messages;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public long getDocuments() { return documents; }
    public void setDocuments(long documents) { this.documents = documents; }

    public long getUsers() { return users; }
    public void setUsers(long users) { this.users = users; }

    public long getMessages() { return messages; }
    public void setMessages(long messages) { this.messages = messages; }
}