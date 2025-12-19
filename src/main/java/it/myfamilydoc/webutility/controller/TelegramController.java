package it.myfamilydoc.webutility.controller;

import it.myfamilydoc.webutility.filter.JwtAuthenticationFilter;
import it.myfamilydoc.webutility.security.UserPrincipal;
import it.myfamilydoc.webutility.dto.TelegramDto;
import it.myfamilydoc.webutility.entity.TelegramMessage.TelegramMessageStatus;
import it.myfamilydoc.webutility.service.TelegramService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Telegram messaging
 * Solo accessibile da utenti con ruolo ADMIN
 * Usa lo stesso sistema di autenticazione JWT del REST Service
 */
@RestController
@RequestMapping("/api/admin/telegram")
@PreAuthorize("hasRole('ADMIN')")
public class TelegramController {

    private static final Logger log = LoggerFactory.getLogger(TelegramController.class);
    
    private final TelegramService telegramService;

    public TelegramController(TelegramService telegramService) {
        this.telegramService = telegramService;
    }

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @Valid @RequestBody TelegramDto.SendMessageRequest request,
            Authentication authentication
    ) {
        log.info("Received request to send Telegram message from user: {}", 
                 authentication.getName());

        try {
            Long userId = getUserIdFromAuthentication(authentication);
            TelegramDto.TelegramMessageResponse response = telegramService.sendMessage(request, userId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Messaggio inviato con successo");
            result.put("data", response);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error sending Telegram message", e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", Map.of(
                "message", "Errore durante l'invio del messaggio: " + e.getMessage(),
                "code", "SEND_MESSAGE_ERROR"
            ));

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/messages")
    public ResponseEntity<Map<String, Object>> getAllMessages() {
        log.info("Fetching all Telegram messages");

        try {
            List<TelegramDto.TelegramMessageResponse> messages = telegramService.getAllMessages();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", messages);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error fetching Telegram messages", e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", Map.of(
                "message", "Errore durante il recupero dei messaggi",
                "code", "FETCH_MESSAGES_ERROR"
            ));

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/messages/{id}")
    public ResponseEntity<Map<String, Object>> getMessageById(@PathVariable Long id) {
        log.info("Fetching Telegram message by ID: {}", id);

        try {
            TelegramDto.TelegramMessageResponse message = telegramService.getMessageById(id);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", message);

            return ResponseEntity.ok(result);

        } catch (RuntimeException e) {
            log.error("Message not found with ID: {}", id);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", Map.of(
                "message", e.getMessage(),
                "code", "MESSAGE_NOT_FOUND"
            ));

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

        } catch (Exception e) {
            log.error("Error fetching message", e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", Map.of(
                "message", "Errore durante il recupero del messaggio",
                "code", "FETCH_MESSAGE_ERROR"
            ));

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/messages/status/{status}")
    public ResponseEntity<Map<String, Object>> getMessagesByStatus(@PathVariable String status) {
        log.info("Fetching Telegram messages by status: {}", status);

        try {
            TelegramMessageStatus messageStatus = TelegramMessageStatus.valueOf(status.toUpperCase());
            List<TelegramDto.TelegramMessageResponse> messages = 
                telegramService.getMessagesByStatus(messageStatus);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", messages);

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            log.error("Invalid status: {}", status);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", Map.of(
                "message", "Status non valido: " + status,
                "code", "INVALID_STATUS"
            ));

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);

        } catch (Exception e) {
            log.error("Error fetching messages by status", e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", Map.of(
                "message", "Errore durante il recupero dei messaggi",
                "code", "FETCH_MESSAGES_ERROR"
            ));

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        log.info("Fetching Telegram message statistics");

        try {
            TelegramDto.TelegramStats stats = telegramService.getStatistics();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", stats);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error fetching statistics", e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", Map.of(
                "message", "Errore durante il recupero delle statistiche",
                "code", "FETCH_STATS_ERROR"
            ));

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/messages/{id}/retry")
    public ResponseEntity<Map<String, Object>> retryMessage(
            @PathVariable Long id,
            Authentication authentication
    ) {
        log.info("Retrying Telegram message with ID: {}", id);

        try {
            Long userId = getUserIdFromAuthentication(authentication);
            TelegramDto.TelegramMessageResponse response = telegramService.retryMessage(id, userId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Messaggio reinviato con successo");
            result.put("data", response);

            return ResponseEntity.ok(result);

        } catch (RuntimeException e) {
            log.error("Message not found for retry: {}", id);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", Map.of(
                "message", e.getMessage(),
                "code", "MESSAGE_NOT_FOUND"
            ));

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

        } catch (Exception e) {
            log.error("Error retrying message", e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", Map.of(
                "message", "Errore durante il reinvio del messaggio",
                "code", "RETRY_MESSAGE_ERROR"
            ));

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @DeleteMapping("/messages/{id}")
    public ResponseEntity<Map<String, Object>> deleteMessage(@PathVariable Long id) {
        log.info("Deleting Telegram message with ID: {}", id);

        try {
            telegramService.deleteMessage(id);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Messaggio eliminato con successo");

            return ResponseEntity.ok(result);

        } catch (RuntimeException e) {
            log.error("Message not found for deletion: {}", id);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", Map.of(
                "message", e.getMessage(),
                "code", "MESSAGE_NOT_FOUND"
            ));

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

        } catch (Exception e) {
            log.error("Error deleting message", e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", Map.of(
                "message", "Errore durante l'eliminazione del messaggio",
                "code", "DELETE_MESSAGE_ERROR"
            ));

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Estrae l'ID utente dall'oggetto Authentication
     * Il Principal è UserPrincipal creato dal JwtAuthenticationFilter
     */
    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            return userPrincipal.getId();
        }
        throw new IllegalStateException("Unable to extract user ID from authentication");
    }
}