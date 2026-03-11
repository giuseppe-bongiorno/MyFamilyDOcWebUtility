package it.myfamilydoc.webutility.controller;

import it.myfamilydoc.webutility.dto.TelegramDto;
import it.myfamilydoc.webutility.entity.TelegramMessage.TelegramMessageStatus;
import it.myfamilydoc.webutility.service.TelegramService;
import it.myfamilydoc.webutility.aop.Auditable;
import it.myfamilydoc.webutility.security.UserPrincipal;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
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
@Validated
public class TelegramController {

    private static final Logger log = LoggerFactory.getLogger(TelegramController.class);

    private final TelegramService telegramService;

    public TelegramController(TelegramService telegramService) {
        this.telegramService = telegramService;
    }

    // ══════════════════════════════════════════════════════════════
    // POST - Invio Messaggi
    // ══════════════════════════════════════════════════════════════

    @PostMapping("/send")
    @Auditable(action = "SEND_TELEGRAM_MESSAGE", entityType = "TelegramMessage")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @Valid @RequestBody TelegramDto.SendMessageRequest request,
            Authentication authentication
    ) {
        log.info("Richiesta invio messaggio Telegram da admin: {}", authentication.getName());

        try {
            Long userId = getUserIdFromAuthentication(authentication);
            TelegramDto.TelegramMessageResponse response = telegramService.sendMessage(request, userId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Messaggio inviato con successo");
            result.put("data", response);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Errore invio messaggio Telegram", e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Errore durante l'invio del messaggio: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // GET - Lettura
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/messages")
    @Auditable(action = "VIEW_ALL_TELEGRAM_MESSAGES", entityType = "TelegramMessage")
    public ResponseEntity<Map<String, Object>> getAllMessages() {
        log.info("Recupero lista messaggi Telegram");

        try {
            List<TelegramDto.TelegramMessageResponse> messages = telegramService.getAllMessages();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", messages);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Errore recupero lista messaggi Telegram", e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Errore durante il recupero dei messaggi");
        }
    }

    @GetMapping("/messages/{id}")
    @Auditable(action = "VIEW_TELEGRAM_MESSAGE", entityType = "TelegramMessage", entityIdParam = "id")
    public ResponseEntity<Map<String, Object>> getMessageById(@PathVariable Long id) {
        log.info("Recupero messaggio Telegram con ID: {}", id);

        try {
            TelegramDto.TelegramMessageResponse message = telegramService.getMessageById(id);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", message);

            return ResponseEntity.ok(result);

        } catch (RuntimeException e) {
            log.error("Messaggio Telegram non trovato con ID: {}", id);
            return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());

        } catch (Exception e) {
            log.error("Errore recupero messaggio Telegram", e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Errore durante il recupero del messaggio");
        }
    }

    @GetMapping("/messages/status/{status}")
    @Auditable(action = "VIEW_TELEGRAM_MESSAGES_BY_STATUS", entityType = "TelegramMessage")
    public ResponseEntity<Map<String, Object>> getMessagesByStatus(@PathVariable String status) {
        log.info("Recupero messaggi Telegram con stato: {}", status);

        try {
            TelegramMessageStatus messageStatus = TelegramMessageStatus.valueOf(status.toUpperCase());
            List<TelegramDto.TelegramMessageResponse> messages =
                    telegramService.getMessagesByStatus(messageStatus);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", messages);

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            log.error("Stato non valido: {}", status);
            return buildErrorResponse(HttpStatus.BAD_REQUEST,
                    "Status non valido: " + status);

        } catch (Exception e) {
            log.error("Errore recupero messaggi per stato", e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Errore durante il recupero dei messaggi");
        }
    }

    @GetMapping("/stats")
    @Auditable(action = "VIEW_TELEGRAM_STATS", entityType = "TelegramMessage")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        log.info("Recupero statistiche messaggi Telegram");

        try {
            TelegramDto.TelegramStats stats = telegramService.getStatistics();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", stats);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Errore recupero statistiche Telegram", e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Errore durante il recupero delle statistiche");
        }
    }

    // ══════════════════════════════════════════════════════════════
    // POST/PUT - Azioni e Modifiche
    // ══════════════════════════════════════════════════════════════

    @PostMapping("/messages/{id}/retry")
    @Auditable(action = "RETRY_TELEGRAM_MESSAGE", entityType = "TelegramMessage", entityIdParam = "id")
    public ResponseEntity<Map<String, Object>> retryMessage(
            @PathVariable Long id,
            Authentication authentication
    ) {
        log.info("Tentativo reinvio messaggio Telegram con ID: {} da admin: {}", id, authentication.getName());

        try {
            Long userId = getUserIdFromAuthentication(authentication);
            TelegramDto.TelegramMessageResponse response = telegramService.retryMessage(id, userId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Messaggio reinviato con successo");
            result.put("data", response);

            return ResponseEntity.ok(result);

        } catch (RuntimeException e) {
            log.error("Messaggio non trovato per reinvio con ID: {}", id);
            return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());

        } catch (Exception e) {
            log.error("Errore reinvio messaggio Telegram", e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Errore durante il reinvio del messaggio");
        }
    }

    // ══════════════════════════════════════════════════════════════
    // DELETE - Eliminazione
    // ══════════════════════════════════════════════════════════════

    @DeleteMapping("/messages/{id}")
    @Auditable(action = "DELETE_TELEGRAM_MESSAGE", entityType = "TelegramMessage", entityIdParam = "id")
    public ResponseEntity<Map<String, Object>> deleteMessage(@PathVariable Long id) {
        log.info("Eliminazione messaggio Telegram con ID: {}", id);

        try {
            telegramService.deleteMessage(id);
            return buildSuccessResponse("Messaggio eliminato con successo");

        } catch (RuntimeException e) {
            log.error("Messaggio non trovato per eliminazione con ID: {}", id);
            return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());

        } catch (Exception e) {
            log.error("Errore eliminazione messaggio Telegram", e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Errore durante l'eliminazione del messaggio");
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Helper Methods
    // ══════════════════════════════════════════════════════════════

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

    private ResponseEntity<Map<String, Object>> buildSuccessResponse(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", message);
        return ResponseEntity.ok(result);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", Map.of("message", message));
        return ResponseEntity.status(status).body(error);
    }
}