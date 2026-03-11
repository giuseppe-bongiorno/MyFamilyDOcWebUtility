package it.myfamilydoc.webutility.controller;

import it.myfamilydoc.webutility.aop.Auditable;
import it.myfamilydoc.webutility.dto.*;
import it.myfamilydoc.webutility.service.DashboardService;
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
 * REST Controller per la Dashboard Admin.
 * 
 * Espone le API necessarie alla pagina AdminDashboardPage.tsx:
 * - GET /api/admin/dashboard/stats           → KPI (utenti, documenti, messaggi, notifiche, sistema)
 * - GET /api/admin/dashboard/health-alerts    → Alert sanitari (pressione/glicemia fuori norma)
 * - GET /api/admin/dashboard/recent-activity  → Attività recente (da audit_log)
 * - GET /api/admin/dashboard/chart-data       → Grafico attività ultimi 30 giorni
 * - GET /api/admin/dashboard/document-distribution → Distribuzione documenti per tipo
 * 
 * Accessibile solo da utenti con ruolo ADMIN.
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    @Auditable(action = "VIEW_DASHBOARD_STATS", entityType = "Dashboard")
    public ResponseEntity<Map<String, Object>> getStats(Authentication authentication) {
        log.info("Richiesta dashboard stats da admin: {}", authentication.getName());

        try {
            DashboardStatsDto stats = dashboardService.getStats();
            return buildDataResponse(stats);
        } catch (Exception e) {
            log.error("Errore recupero dashboard stats", e);
            return buildErrorResponse("Errore durante il recupero delle statistiche: " + e.getMessage());
        }
    }

    @GetMapping("/health-alerts")
    @Auditable(action = "VIEW_HEALTH_ALERTS", entityType = "Dashboard")
    public ResponseEntity<Map<String, Object>> getHealthAlerts(Authentication authentication) {
        log.info("Richiesta health alerts da admin: {}", authentication.getName());

        try {
            List<HealthAlertDto> alerts = dashboardService.getHealthAlerts();
            return buildDataResponse(alerts);
        } catch (Exception e) {
            log.error("Errore recupero health alerts", e);
            return buildErrorResponse("Errore durante il recupero degli alert sanitari: " + e.getMessage());
        }
    }

    @GetMapping("/recent-activity")
    @Auditable(action = "VIEW_RECENT_ACTIVITY", entityType = "Dashboard")
    public ResponseEntity<Map<String, Object>> getRecentActivity(Authentication authentication) {
        log.info("Richiesta attività recente da admin: {}", authentication.getName());

        try {
            List<RecentActivityDto> activity = dashboardService.getRecentActivity();
            return buildDataResponse(activity);
        } catch (Exception e) {
            log.error("Errore recupero attività recente", e);
            return buildErrorResponse("Errore durante il recupero delle attività recenti: " + e.getMessage());
        }
    }

    @GetMapping("/chart-data")
    @Auditable(action = "VIEW_CHART_DATA", entityType = "Dashboard")
    public ResponseEntity<Map<String, Object>> getChartData(Authentication authentication) {
        log.info("Richiesta chart data da admin: {}", authentication.getName());

        try {
            List<ChartDataPointDto> chartData = dashboardService.getChartData();
            return buildDataResponse(chartData);
        } catch (Exception e) {
            log.error("Errore recupero chart data", e);
            return buildErrorResponse("Errore durante il recupero dei dati per il grafico: " + e.getMessage());
        }
    }

    @GetMapping("/document-distribution")
    @Auditable(action = "VIEW_DOCUMENT_DISTRIBUTION", entityType = "Dashboard")
    public ResponseEntity<Map<String, Object>> getDocumentDistribution(Authentication authentication) {
        log.info("Richiesta distribuzione documenti da admin: {}", authentication.getName());

        try {
            List<DocumentDistributionDto> distribution = dashboardService.getDocumentDistribution();
            return buildDataResponse(distribution);
        } catch (Exception e) {
            log.error("Errore recupero distribuzione documenti", e);
            return buildErrorResponse("Errore durante il recupero della distribuzione: " + e.getMessage());
        }
    }

    @GetMapping("/notification-history")
    @Auditable(action = "VIEW_NOTIFICATION_HISTORY", entityType = "Dashboard")
    public ResponseEntity<Map<String, Object>> getNotificationHistory(
            @RequestParam(required = false, defaultValue = "50") int limit,
            Authentication authentication) {
        log.info("Richiesta cronologia notifiche da admin: {}", authentication.getName());

        try {
            List<NotificationHistoryDto> history = dashboardService.getNotificationHistory(limit);
            return buildDataResponse(history);
        } catch (Exception e) {
            log.error("Errore recupero cronologia notifiche", e);
            return buildErrorResponse("Errore durante il recupero della cronologia: " + e.getMessage());
        }
    }

    // ── Response Helpers ─────────────────────────────────────────

    private ResponseEntity<Map<String, Object>> buildDataResponse(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", data);
        return ResponseEntity.ok(result);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", Map.of("message", message));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}