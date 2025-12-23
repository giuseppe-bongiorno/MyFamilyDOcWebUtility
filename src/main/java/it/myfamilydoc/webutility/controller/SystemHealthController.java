package it.myfamilydoc.webutility.controller;

import it.myfamilydoc.webutility.dto.ContainerHealthDto;
import it.myfamilydoc.webutility.service.DockerMonitorService;
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
 * REST Controller for System Health
 * Solo accessibile da utenti con ruolo ADMIN
 * Usa lo stesso sistema di autenticazione JWT del REST Service
 */
@RestController
@RequestMapping("/api/admin/system-health")
@PreAuthorize("hasRole('ADMIN')")
public class SystemHealthController {

    private static final Logger log = LoggerFactory.getLogger(SystemHealthController.class);

    private final DockerMonitorService dockerMonitorService;

    public SystemHealthController(DockerMonitorService dockerMonitorService) {
        this.dockerMonitorService = dockerMonitorService;
    }

    @GetMapping
    public ResponseEntity<Map<String,Object>> getSystemHealth(Authentication authentication) {
        log.info("Received system health request from user: {}", authentication.getName());

        try {
            // Chiamo il servizio avanzato che restituisce metriche reali
            List<ContainerHealthDto> containers = dockerMonitorService.getAllContainersHealth();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", containers);
            result.put("count", containers.size());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error fetching system health", e);

            Map<String,Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", Map.of(
                "message", "Errore durante la lettura dello stato dei container: " + e.getMessage(),
                "code", "SYSTEM_HEALTH_ERROR"
            ));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
