package it.myfamilydoc.webutility.controller;

import it.myfamilydoc.webutility.dto.ChangeRoleRequest;
import it.myfamilydoc.webutility.dto.DeleteUserRequest;
import it.myfamilydoc.webutility.dto.UserManagementDto;
import it.myfamilydoc.webutility.dto.UserStatsDto;
import it.myfamilydoc.webutility.service.UserManagementService;
import it.myfamilydoc.webutility.aop.Auditable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
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
 * REST Controller per la gestione utenti (Admin Dashboard).
 * 
 * Espone le API necessarie alla pagina UserManagementPage.tsx:
 * - GET  /api/admin/users              → Lista utenti con filtri
 * - GET  /api/admin/users/stats        → Statistiche utenti
 * - PUT  /api/admin/users/{id}/enable  → Abilita utente
 * - PUT  /api/admin/users/{id}/disable → Disabilita utente
 * - PUT  /api/admin/users/{id}/verify-email → Verifica email (admin override)
 * - POST /api/admin/users/{id}/reset-password → Invia reset password
 * - PUT  /api/admin/users/{id}/role    → Cambia ruolo
 * - DELETE /api/admin/users/{id}       → Soft-delete utente
 * 
 * Accessibile solo da utenti con ruolo ADMIN.
 * Stesso sistema di autenticazione JWT del REST Service.
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class UserManagementController {

    private static final Logger log = LoggerFactory.getLogger(UserManagementController.class);

    private final UserManagementService userManagementService;

    public UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    // ══════════════════════════════════════════════════════════════
    // GET - Lettura
    // ══════════════════════════════════════════════════════════════

    @GetMapping
    @Auditable(action = "VIEW_ALL_USERS", entityType = "User")
    public ResponseEntity<Map<String, Object>> getUsers(
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false, defaultValue = "ALL") String role,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false, defaultValue = "ALL") String emailVerified,
            Authentication authentication) {

        log.info("Richiesta lista utenti da admin: {} - filtri: search={}, role={}, status={}, emailVerified={}",
                authentication.getName(), search, role, status, emailVerified);

        try {
            List<UserManagementDto> users = userManagementService.getUsers(search, role, status, emailVerified);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", users);
            result.put("count", users.size());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Errore recupero lista utenti", e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Errore durante il recupero degli utenti: " + e.getMessage());
        }
    }

    @GetMapping("/stats")
    @Auditable(action = "VIEW_USER_STATS", entityType = "User")
    public ResponseEntity<Map<String, Object>> getUserStats(Authentication authentication) {
        log.info("Richiesta statistiche utenti da admin: {}", authentication.getName());

        try {
            UserStatsDto stats = userManagementService.getUserStats();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", stats);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Errore recupero statistiche utenti", e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Errore durante il recupero delle statistiche: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════
    // PUT - Modifiche
    // ══════════════════════════════════════════════════════════════

    @PutMapping("/{userId}/enable")
    @Auditable(action = "ENABLE_USER", entityType = "User", entityIdParam = "userId")
    public ResponseEntity<Map<String, Object>> enableUser(
            @PathVariable @Positive Long userId,
            Authentication authentication) {

        log.info("Admin {} richiede abilitazione utente ID: {}", authentication.getName(), userId);

        try {
            userManagementService.enableUser(userId);
            return buildSuccessResponse("Utente abilitato con successo");
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            log.error("Errore abilitazione utente {}", userId, e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Errore durante l'abilitazione dell'utente");
        }
    }

    @PutMapping("/{userId}/disable")
    @Auditable(action = "DISABLE_USER", entityType = "User", entityIdParam = "userId")
    public ResponseEntity<Map<String, Object>> disableUser(
            @PathVariable @Positive Long userId,
            Authentication authentication) {

        log.info("Admin {} richiede disabilitazione utente ID: {}", authentication.getName(), userId);

        try {
            userManagementService.disableUser(userId);
            return buildSuccessResponse("Utente disabilitato con successo");
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            log.error("Errore disabilitazione utente {}", userId, e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Errore durante la disabilitazione dell'utente");
        }
    }

    @PutMapping("/{userId}/verify-email")
    @Auditable(action = "ADMIN_VERIFY_EMAIL", entityType = "User", entityIdParam = "userId")
    public ResponseEntity<Map<String, Object>> verifyEmail(
            @PathVariable @Positive Long userId,
            Authentication authentication) {

        log.info("Admin {} richiede verifica email per utente ID: {}", authentication.getName(), userId);

        try {
            userManagementService.verifyEmail(userId);
            return buildSuccessResponse("Email verificata con successo");
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            log.error("Errore verifica email utente {}", userId, e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Errore durante la verifica dell'email");
        }
    }

    @PutMapping("/{userId}/role")
    @Auditable(action = "CHANGE_USER_ROLE", entityType = "User", entityIdParam = "userId")
    public ResponseEntity<Map<String, Object>> changeRole(
            @PathVariable @Positive Long userId,
            @Valid @RequestBody ChangeRoleRequest request,
            Authentication authentication) {

        log.info("Admin {} richiede cambio ruolo per utente ID: {} -> {}",
                authentication.getName(), userId, request.getRole());

        try {
            userManagementService.changeRole(userId, request.getRole());
            return buildSuccessResponse("Ruolo cambiato in " + request.getRole() + " con successo");
        } catch (IllegalArgumentException e) {
            HttpStatus status = e.getMessage().contains("Ruolo non trovato")
                    ? HttpStatus.BAD_REQUEST
                    : HttpStatus.NOT_FOUND;
            return buildErrorResponse(status, e.getMessage());
        } catch (Exception e) {
            log.error("Errore cambio ruolo utente {}", userId, e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Errore durante il cambio ruolo");
        }
    }

    // ══════════════════════════════════════════════════════════════
    // POST - Azioni
    // ══════════════════════════════════════════════════════════════

    @PostMapping("/{userId}/reset-password")
    @Auditable(action = "ADMIN_RESET_PASSWORD", entityType = "User", entityIdParam = "userId")
    public ResponseEntity<Map<String, Object>> resetPassword(
            @PathVariable @Positive Long userId,
            Authentication authentication) {

        log.info("Admin {} richiede reset password per utente ID: {}", authentication.getName(), userId);

        try {
            userManagementService.resetPassword(userId);
            return buildSuccessResponse("Email di reset password inviata con successo");
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            log.error("Errore reset password utente {}", userId, e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Errore durante il reset della password");
        }
    }

    // ══════════════════════════════════════════════════════════════
    // DELETE - Eliminazione
    // ══════════════════════════════════════════════════════════════

    @DeleteMapping("/{userId}")
    @Auditable(action = "DELETE_USER", entityType = "User", entityIdParam = "userId")
    public ResponseEntity<Map<String, Object>> deleteUser(
            @PathVariable @Positive Long userId,
            @RequestBody(required = false) DeleteUserRequest request,
            Authentication authentication) {

        log.info("Admin {} richiede eliminazione utente ID: {}", authentication.getName(), userId);

        try {
            String reason = (request != null && request.getReason() != null)
                    ? request.getReason()
                    : "Eliminato dall'amministratore";
            userManagementService.deleteUser(userId, reason);
            return buildSuccessResponse("Utente eliminato con successo");
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            log.error("Errore eliminazione utente {}", userId, e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Errore durante l'eliminazione dell'utente");
        }
    }

    // ══════════════════════════════════════════════════════════════
    // PUT - Ripristino e Anonimizzazione
    // ══════════════════════════════════════════════════════════════

    @PutMapping("/{userId}/restore")
    @Auditable(action = "RESTORE_USER", entityType = "User", entityIdParam = "userId")
    public ResponseEntity<Map<String, Object>> restoreUser(
            @PathVariable @Positive Long userId,
            Authentication authentication) {

        log.info("Admin {} richiede ripristino utente ID: {}", authentication.getName(), userId);

        try {
            userManagementService.restoreUser(userId);
            return buildSuccessResponse("Utente ripristinato con successo");
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("Errore ripristino utente {}", userId, e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Errore durante il ripristino dell'utente");
        }
    }

    @PutMapping("/{userId}/anonymize")
    @Auditable(action = "ANONYMIZE_USER_GDPR", entityType = "User", entityIdParam = "userId")
    public ResponseEntity<Map<String, Object>> anonymizeUser(
            @PathVariable @Positive Long userId,
            Authentication authentication) {

        log.info("Admin {} richiede anonimizzazione utente ID: {}", authentication.getName(), userId);

        try {
            userManagementService.anonymizeUser(userId);
            return buildSuccessResponse("Utente anonimizzato definitivamente (GDPR Art. 17)");
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("Errore anonimizzazione utente {}", userId, e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Errore durante l'anonimizzazione dell'utente");
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Response Helpers
    // ══════════════════════════════════════════════════════════════

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