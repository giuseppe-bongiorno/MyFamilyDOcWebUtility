package it.myfamilydoc.webutility.service;

import it.myfamilydoc.webutility.model.AuditLogEntity;
import it.myfamilydoc.webutility.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Servizio di Audit Logging per il Web Utility Service.
 * 
 * Persiste gli eventi di audit sulla tabella auth_schema.audit_log
 * (stessa tabella condivisa con il REST Service / Auth Service).
 * 
 * Funzionalità GDPR:
 * - IP pseudonymization: l'IP viene hashato (SHA-256) nel campo ip_hash
 * - Correlation ID: dal header X-Correlation-ID o generato automaticamente
 * - Integrity hash: SHA-256 di action+username+entityId+status+timestamp
 *   per rilevare manomissioni del record
 */
@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired(required = false)
    private HttpServletRequest request;

    /**
     * Registra un evento di audit completo (usato dall'AuditLoggingAspect).
     */
    public void log(String action, String username, String ip,
                    String entityType, String entityId,
                    String status, String details) {
        try {
            LocalDateTime now = LocalDateTime.now();
            String correlationId = resolveCorrelationId();
            String ipHash = hashSha256(ip);
            String integrityHash = computeIntegrityHash(action, username, entityId, status, now);

            // Persisti su DB
            AuditLogEntity entity = new AuditLogEntity();
            entity.setAction(action);
            entity.setUsername(username);
            entity.setIpAddress(ip);
            entity.setEntityType(entityType);
            entity.setEntityId(entityId);
            entity.setStatus(status);
            entity.setDetails(details);
            entity.setCreatedAt(now);
            entity.setCorrelationId(correlationId);
            entity.setIpHash(ipHash);
            entity.setIntegrityHash(integrityHash);

            auditLogRepository.save(entity);

            // Log anche su SLF4J per monitoring/alerting
            auditLog.info("[AUDIT] action={} | user={} | ip={} | entityType={} | entityId={} | status={} | correlationId={} | details={}",
                    action, username, ip, entityType, entityId, status, correlationId, details);

        } catch (Exception e) {
            // L'audit non deve MAI bloccare l'operazione principale
            log.error("Errore durante la registrazione dell'audit log: action={}, username={}, error={}",
                    action, username, e.getMessage(), e);
        }
    }

    /**
     * Overload semplificato per log con solo username e messaggio.
     */
    public void log(String username, String message) {
        String ip = resolveIp();
        log("GENERIC", username, ip, null, null, "INFO", message);
    }

    // ══════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════

    /**
     * Risolve il Correlation ID dal header X-Correlation-ID.
     * Se non presente, ne genera uno nuovo.
     */
    private String resolveCorrelationId() {
        try {
            if (request != null) {
                String correlationId = request.getHeader("X-Correlation-ID");
                if (correlationId != null && !correlationId.isBlank()) {
                    return correlationId;
                }
            }
        } catch (Exception e) {
            // Può fallire se chiamato fuori dal contesto HTTP (es. @Async)
        }
        return UUID.randomUUID().toString();
    }

    /**
     * Risolve l'IP dalla richiesta HTTP corrente.
     */
    private String resolveIp() {
        try {
            if (request != null) {
                // Controlla header proxy/load balancer
                String forwarded = request.getHeader("X-Forwarded-For");
                if (forwarded != null && !forwarded.isBlank()) {
                    return forwarded.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            // Fuori dal contesto HTTP
        }
        return "unknown";
    }

    /**
     * SHA-256 hash per pseudonimizzazione IP (GDPR).
     */
    private String hashSha256(String input) {
        if (input == null || input.isBlank()) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 non disponibile", e);
            return null;
        }
    }

    /**
     * Calcola l'integrity hash del record audit.
     * Concatena i campi chiave e calcola SHA-256 per rilevare manomissioni.
     */
    private String computeIntegrityHash(String action, String username,
                                         String entityId, String status,
                                         LocalDateTime timestamp) {
        String payload = String.join("|",
                action != null ? action : "",
                username != null ? username : "",
                entityId != null ? entityId : "",
                status != null ? status : "",
                timestamp != null ? timestamp.toString() : ""
        );
        return hashSha256(payload);
    }
}