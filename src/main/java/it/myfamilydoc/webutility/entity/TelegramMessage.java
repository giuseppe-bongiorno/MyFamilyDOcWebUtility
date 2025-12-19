package it.myfamilydoc.webutility.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing a Telegram message sent via bot
 */
@Entity
@Table(name = "telegram_messages", schema = "web_utility")
public class TelegramMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bot_token", nullable = false, length = 255)
    private String botToken;

    @Column(name = "chat_id", nullable = false, length = 100)
    private String chatId;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TelegramMessageStatus status;

    @Column(name = "telegram_message_id")
    private Long telegramMessageId;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public TelegramMessage() {
    }

    public TelegramMessage(Long id, String botToken, String chatId, String message, 
                          TelegramMessageStatus status, Long telegramMessageId, 
                          LocalDateTime sentAt, String errorMessage, Long createdBy, 
                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.botToken = botToken;
        this.chatId = chatId;
        this.message = message;
        this.status = status;
        this.telegramMessageId = telegramMessageId;
        this.sentAt = sentAt;
        this.errorMessage = errorMessage;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBotToken() {
        return botToken;
    }

    public void setBotToken(String botToken) {
        this.botToken = botToken;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public TelegramMessageStatus getStatus() {
        return status;
    }

    public void setStatus(TelegramMessageStatus status) {
        this.status = status;
    }

    public Long getTelegramMessageId() {
        return telegramMessageId;
    }

    public void setTelegramMessageId(Long telegramMessageId) {
        this.telegramMessageId = telegramMessageId;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TelegramMessage that = (TelegramMessage) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TelegramMessage{" +
                "id=" + id +
                ", chatId='" + chatId + '\'' +
                ", status=" + status +
                ", telegramMessageId=" + telegramMessageId +
                ", sentAt=" + sentAt +
                ", createdBy=" + createdBy +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    /**
     * Enum for Telegram message status
     */
    public enum TelegramMessageStatus {
        PENDING,
        SENT,
        FAILED
    }
}