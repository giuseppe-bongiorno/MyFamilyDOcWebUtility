package it.myfamilydoc.webutility.dto;

import it.myfamilydoc.webutility.entity.TelegramMessage.TelegramMessageStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * DTOs for Telegram messaging service
 */
public class TelegramDto {

    /**
     * Request DTO for sending a Telegram message
     */
    public static class SendMessageRequest {
        
        @NotBlank(message = "Bot token is required")
        @Size(max = 255, message = "Bot token must be less than 255 characters")
        private String botToken;

        @NotBlank(message = "Chat ID is required")
        @Size(max = 100, message = "Chat ID must be less than 100 characters")
        private String chatId;

        @NotBlank(message = "Message is required")
        private String message;

        public SendMessageRequest() {
        }

        public SendMessageRequest(String botToken, String chatId, String message) {
            this.botToken = botToken;
            this.chatId = chatId;
            this.message = message;
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
    }

    /**
     * Response DTO for Telegram message
     */
    public static class TelegramMessageResponse {
        private Long id;
        private String botToken;
        private String chatId;
        private String message;
        private TelegramMessageStatus status;
        private Long telegramMessageId;
        private LocalDateTime sentAt;
        private String errorMessage;
        private Long createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public TelegramMessageResponse() {
        }

        public TelegramMessageResponse(Long id, String botToken, String chatId, String message, 
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
    }

    /**
     * Statistics DTO for Telegram messages
     */
    public static class TelegramStats {
        private Long total;
        private Long sent;
        private Long failed;
        private Long pending;

        public TelegramStats() {
        }

        public TelegramStats(Long total, Long sent, Long failed, Long pending) {
            this.total = total;
            this.sent = sent;
            this.failed = failed;
            this.pending = pending;
        }

        public Long getTotal() {
            return total;
        }

        public void setTotal(Long total) {
            this.total = total;
        }

        public Long getSent() {
            return sent;
        }

        public void setSent(Long sent) {
            this.sent = sent;
        }

        public Long getFailed() {
            return failed;
        }

        public void setFailed(Long failed) {
            this.failed = failed;
        }

        public Long getPending() {
            return pending;
        }

        public void setPending(Long pending) {
            this.pending = pending;
        }
    }

    /**
     * Telegram API response from Bot API
     */
    public static class TelegramApiResponse {
        private Boolean ok;
        private TelegramApiResult result;
        private String description;
        private Integer errorCode;

        public TelegramApiResponse() {
        }

        public TelegramApiResponse(Boolean ok, TelegramApiResult result, String description, Integer errorCode) {
            this.ok = ok;
            this.result = result;
            this.description = description;
            this.errorCode = errorCode;
        }

        public Boolean getOk() {
            return ok;
        }

        public void setOk(Boolean ok) {
            this.ok = ok;
        }

        public TelegramApiResult getResult() {
            return result;
        }

        public void setResult(TelegramApiResult result) {
            this.result = result;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Integer getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(Integer errorCode) {
            this.errorCode = errorCode;
        }

        public static class TelegramApiResult {
            private Long messageId;
            private TelegramApiChat chat;
            private String text;
            private Integer date;

            public TelegramApiResult() {
            }

            public TelegramApiResult(Long messageId, TelegramApiChat chat, String text, Integer date) {
                this.messageId = messageId;
                this.chat = chat;
                this.text = text;
                this.date = date;
            }

            public Long getMessageId() {
                return messageId;
            }

            public void setMessageId(Long messageId) {
                this.messageId = messageId;
            }

            public TelegramApiChat getChat() {
                return chat;
            }

            public void setChat(TelegramApiChat chat) {
                this.chat = chat;
            }

            public String getText() {
                return text;
            }

            public void setText(String text) {
                this.text = text;
            }

            public Integer getDate() {
                return date;
            }

            public void setDate(Integer date) {
                this.date = date;
            }
        }

        public static class TelegramApiChat {
            private Long id;
            private String type;
            private String title;
            private String username;

            public TelegramApiChat() {
            }

            public TelegramApiChat(Long id, String type, String title, String username) {
                this.id = id;
                this.type = type;
                this.title = title;
                this.username = username;
            }

            public Long getId() {
                return id;
            }

            public void setId(Long id) {
                this.id = id;
            }

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public String getTitle() {
                return title;
            }

            public void setTitle(String title) {
                this.title = title;
            }

            public String getUsername() {
                return username;
            }

            public void setUsername(String username) {
                this.username = username;
            }
        }
    }
}