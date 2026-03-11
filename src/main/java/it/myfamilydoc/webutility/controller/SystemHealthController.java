package it.myfamilydoc.webutility.controller;

import it.myfamilydoc.webutility.dto.ContainerHealthDto;
import it.myfamilydoc.webutility.service.DockerMonitorService;
import it.myfamilydoc.webutility.aop.Auditable;
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
 * REST Controller for System Health
 * Solo accessibile da utenti con ruolo ADMIN
 * Usa lo stesso sistema di autenticazione JWT del REST Service
 */
@RestController
@RequestMapping("/api/admin/system-health")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class SystemHealthController {

    private static final Logger log = LoggerFactory.getLogger(SystemHealthController.class);

    private final DockerMonitorService dockerMonitorService;

    public SystemHealthController(DockerMonitorService dockerMonitorService) {
        this.dockerMonitorService = dockerMonitorService;
    }

    // ══════════════════════════════════════════════════════════════
    // GET - Lettura
    // ══════════════════════════════════════════════════════════════

    @GetMapping
    @Auditable(action = "VIEW_SYSTEM_HEALTH", entityType = "System")
    public ResponseEntity<Map<String, Object>> getSystemHealth(Authentication authentication) {
        log.info("Richiesta stato salute sistema da admin: {}", authentication.getName());

        try {
            // Chiamo il servizio avanzato che restituisce metriche reali
            List<ContainerHealthDto> containers = dockerMonitorService.getAllContainersHealth();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", containers);
            result.put("count", containers.size());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Errore recupero stato salute sistema", e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Errore durante la lettura dello stato dei container: " + e.getMessage());
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