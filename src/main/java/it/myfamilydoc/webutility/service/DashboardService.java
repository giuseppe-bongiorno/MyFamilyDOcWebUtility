package it.myfamilydoc.webutility.service;

import it.myfamilydoc.webutility.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service per la dashboard admin.
 * 
 * Usa JdbcTemplate per query cross-schema (auth_schema + medical_data)
 * su un'unica connessione PostgreSQL.
 * 
 * Ogni query è racchiusa in try-catch: se una tabella non esiste
 * o la struttura è diversa, il metodo restituisce 0 o dati vuoti
 * senza bloccare il resto della dashboard.
 */
@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final JdbcTemplate jdbcTemplate;

    public DashboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ══════════════════════════════════════════════════════════════
    // 1. STATS (KPI Cards)
    // ══════════════════════════════════════════════════════════════

    public DashboardStatsDto getStats() {
        DashboardStatsDto stats = new DashboardStatsDto();

        // ── Utenti ───────────────────────────────────────────────
        DashboardStatsDto.UserStatsSection users = stats.getUsers();
        users.setTotal(countSafe("SELECT COUNT(*) FROM auth_schema.users WHERE deleted_at IS NULL"));
        users.setTrend(computeTrend(
                "SELECT COUNT(*) FROM auth_schema.users WHERE deleted_at IS NULL AND created_at >= date_trunc('month', CURRENT_DATE)",
                "SELECT COUNT(*) FROM auth_schema.users WHERE deleted_at IS NULL AND created_at >= date_trunc('month', CURRENT_DATE) - INTERVAL '1 month' AND created_at < date_trunc('month', CURRENT_DATE)"
        ));

        // ── Documenti (somma di tutte le tabelle) ────────────────
        DashboardStatsDto.DocumentStatsSection documents = stats.getDocuments();
        long totalDocs = countAllDocuments();
        long docsThisMonth = countAllDocumentsInPeriod("date_trunc('month', CURRENT_DATE)", "CURRENT_DATE");
        long docsLastMonth = countAllDocumentsInPeriod(
                "date_trunc('month', CURRENT_DATE) - INTERVAL '1 month'",
                "date_trunc('month', CURRENT_DATE)"
        );
        documents.setTotal(totalDocs);
        documents.setTrend(calculateTrendPercent(docsThisMonth, docsLastMonth));

        // ── Messaggi ─────────────────────────────────────────────
        DashboardStatsDto.MessageStatsSection messages = stats.getMessages();
        messages.setTotal(countSafe("SELECT COUNT(*) FROM medical_data.messaggi"));
        messages.setTrend(computeTrend(
                "SELECT COUNT(*) FROM medical_data.messaggi WHERE data_invio >= date_trunc('month', CURRENT_DATE)",
                "SELECT COUNT(*) FROM medical_data.messaggi WHERE data_invio >= date_trunc('month', CURRENT_DATE) - INTERVAL '1 month' AND data_invio < date_trunc('month', CURRENT_DATE)"
        ));

        // ── Notifiche (da push_notification_log) ──
        DashboardStatsDto.NotificationStatsSection notifications = stats.getNotifications();
        notifications.setSent(countSafe(
                "SELECT COUNT(*) FROM medical_data.push_notification_log"
        ));
        notifications.setTrend(computeTrend(
                "SELECT COUNT(*) FROM medical_data.push_notification_log WHERE created_at >= date_trunc('month', CURRENT_DATE)",
                "SELECT COUNT(*) FROM medical_data.push_notification_log WHERE created_at >= date_trunc('month', CURRENT_DATE) - INTERVAL '1 month' AND created_at < date_trunc('month', CURRENT_DATE)"
        ));
        
        

        // ── Sistema ──────────────────────────────────────────────
        DashboardStatsDto.SystemStatsSection system = stats.getSystem();
        system.setUptime(calculateUptime());
        system.setActiveDevices(countSafe(
                "SELECT COUNT(DISTINCT id) FROM auth_schema.users WHERE last_login_at >= NOW() - INTERVAL '24 hours' AND deleted_at IS NULL"
        ));
        system.setStorageUsed(getDatabaseSizeGB());
        system.setStorageTotal(50.0); // Configurabile via application.properties
        system.setApiCalls24h(countSafe(
                "SELECT COUNT(*) FROM auth_schema.audit_log WHERE created_at >= NOW() - INTERVAL '24 hours'"
        ));

        return stats;
    }

    // ══════════════════════════════════════════════════════════════
    // 2. HEALTH ALERTS (Pressione/Glicemia fuori norma)
    // ══════════════════════════════════════════════════════════════

    public List<HealthAlertDto> getHealthAlerts() {
        List<HealthAlertDto> alerts = new ArrayList<>();

        // Pressione arteriosa fuori norma (ultime 48h)
        // Critico: sistolica > 180 o diastolica > 120
        // Warning: sistolica > 140 o diastolica > 90
        try {
            List<HealthAlertDto> pressureAlerts = jdbcTemplate.query(
                    """
                    SELECT p.id, u.username,
                           p.sistolica, p.diastolica,
                           p.created_at::text AS timestamp
                    FROM medical_data.pressione_arteriosa p
                    JOIN auth_schema.users u ON u.id = p.user_id
                    WHERE p.created_at >= NOW() - INTERVAL '48 hours'
                      AND (p.sistolica > 140 OR p.diastolica > 90
                           OR p.sistolica < 90 OR p.diastolica < 60)
                    ORDER BY p.created_at DESC
                    LIMIT 20
                    """,
                    (rs, rowNum) -> {
                        int sistolica = rs.getInt("sistolica");
                        int diastolica = rs.getInt("diastolica");
                        String severity = (sistolica > 180 || diastolica > 120 || sistolica < 80)
                                ? "critical" : "warning";
                        String message = String.format("Pressione: %d/%d mmHg", sistolica, diastolica);

                        return new HealthAlertDto(
                                rs.getLong("id"),
                                severity,
                                rs.getString("username"),
                                message,
                                rs.getString("timestamp")
                        );
                    }
            );
            alerts.addAll(pressureAlerts);
        } catch (Exception e) {
            log.warn("Errore query pressione_arteriosa per health alerts: {}", e.getMessage());
        }

        // Glicemia fuori norma (ultime 48h)
        // Critico: > 250 o < 54 mg/dL
        // Warning: > 126 o < 70 mg/dL
        try {
            List<HealthAlertDto> glucoseAlerts = jdbcTemplate.query(
                    """
                    SELECT g.id, u.username,
                           g.valore,
                           g.created_at::text AS timestamp
                    FROM medical_data.glicemia g
                    JOIN auth_schema.users u ON u.id = g.user_id
                    WHERE g.created_at >= NOW() - INTERVAL '48 hours'
                      AND (g.valore > 126 OR g.valore < 70)
                    ORDER BY g.created_at DESC
                    LIMIT 20
                    """,
                    (rs, rowNum) -> {
                        int valore = rs.getInt("valore");
                        String severity = (valore > 250 || valore < 54) ? "critical" : "warning";
                        String message = String.format("Glicemia: %d mg/dL", valore);

                        return new HealthAlertDto(
                                rs.getLong("id"),
                                severity,
                                rs.getString("username"),
                                message,
                                rs.getString("timestamp")
                        );
                    }
            );
            alerts.addAll(glucoseAlerts);
        } catch (Exception e) {
            log.warn("Errore query glicemia per health alerts: {}", e.getMessage());
        }

        // Ordina per timestamp decrescente
        alerts.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));

        return alerts;
    }

    // ══════════════════════════════════════════════════════════════
    // 3. RECENT ACTIVITY (da audit_log)
    // ══════════════════════════════════════════════════════════════

    public List<RecentActivityDto> getRecentActivity() {
        try {
            return jdbcTemplate.query(
                    """
                    SELECT id, action, username, details, created_at::text AS timestamp
                    FROM auth_schema.audit_log
                    WHERE created_at >= NOW() - INTERVAL '7 days'
                      AND deleted_at IS NULL
                    ORDER BY created_at DESC
                    LIMIT 30
                    """,
                    (rs, rowNum) -> {
                        String action = rs.getString("action");
                        String username = rs.getString("username");
                        String details = rs.getString("details");

                        // Mappa action → tipo attività per il frontend
                        String type = mapActionToActivityType(action);
                        String description = buildActivityDescription(action, username, details);

                        return new RecentActivityDto(
                                rs.getLong("id"),
                                type,
                                description,
                                rs.getString("timestamp")
                        );
                    }
            );
        } catch (Exception e) {
            log.warn("Errore query attività recenti: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ══════════════════════════════════════════════════════════════
    // 4. CHART DATA (Attività ultimi 30 giorni)
    // ══════════════════════════════════════════════════════════════

    public List<ChartDataPointDto> getChartData() {
        List<ChartDataPointDto> chartData = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(29);

        // Pre-popola tutti i 30 giorni a 0
        Map<String, ChartDataPointDto> dayMap = new LinkedHashMap<>();
        for (int i = 0; i < 30; i++) {
            String date = startDate.plusDays(i).format(ISO_DATE);
            dayMap.put(date, new ChartDataPointDto(date, 0, 0, 0));
        }

        // Nuovi utenti per giorno
        fillDailyCount(dayMap, "users",
                "SELECT created_at::date AS day, COUNT(*) AS cnt FROM auth_schema.users " +
                "WHERE created_at >= ? AND deleted_at IS NULL GROUP BY created_at::date",
                startDate
        );

        // Documenti per giorno (somma tutte le tabelle)
        String[] docTables = {
                "medical_data.visite_mediche",
                "medical_data.ricette",
                "medical_data.esenzioni",
                "medical_data.certificati_medici",
                "medical_data.vaccini",
                "medical_data.scontrini",
                "medical_data.invalidita",
                "medical_data.referti"
        };
        for (String table : docTables) {
            fillDailyCount(dayMap, "documents",
                    "SELECT created_at::date AS day, COUNT(*) AS cnt FROM " + table +
                    " WHERE created_at >= ? GROUP BY created_at::date",
                    startDate
            );
        }

        // Messaggi per giorno
        fillDailyCount(dayMap, "messages",
                "SELECT data_invio::date AS day, COUNT(*) AS cnt FROM medical_data.messaggi " +
                "WHERE data_invio >= ? GROUP BY data_invio::date",
                startDate
        );

        chartData.addAll(dayMap.values());
        return chartData;
    }

    // ══════════════════════════════════════════════════════════════
    // 5. DOCUMENT DISTRIBUTION (Pie chart)
    // ══════════════════════════════════════════════════════════════

    public List<DocumentDistributionDto> getDocumentDistribution() {
        List<DocumentDistributionDto> distribution = new ArrayList<>();

        Map<String, String> tableLabels = new LinkedHashMap<>();
        tableLabels.put("medical_data.visite_mediche", "Visite Mediche");
        tableLabels.put("medical_data.ricette", "Ricette");
        tableLabels.put("medical_data.esenzioni", "Esenzioni");
        tableLabels.put("medical_data.certificati_medici", "Certificati");
        tableLabels.put("medical_data.vaccini", "Vaccini");
        tableLabels.put("medical_data.scontrini", "Scontrini");
        tableLabels.put("medical_data.invalidita", "Invalidità");
        tableLabels.put("medical_data.referti", "Referti");

        tableLabels.forEach((table, label) -> {
            long count = countSafe("SELECT COUNT(*) FROM " + table);
            if (count > 0) {
                distribution.add(new DocumentDistributionDto(label, count));
            }
        });

        return distribution;
    }

    // ══════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════

    /**
     * Esegue una COUNT query in modo safe. Se fallisce restituisce 0.
     */
    private long countSafe(String sql) {
        try {
            Long result = jdbcTemplate.queryForObject(sql, Long.class);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.warn("Query count fallita: {} - errore: {}", sql.substring(0, Math.min(sql.length(), 80)), e.getMessage());
            return 0;
        }
    }

    /**
     * Conta tutti i documenti medici sommando tutte le tabelle.
     */
    private long countAllDocuments() {
        String[] tables = {
                "medical_data.visite_mediche", "medical_data.ricette",
                "medical_data.esenzioni", "medical_data.certificati_medici",
                "medical_data.vaccini", "medical_data.scontrini",
                "medical_data.invalidita", "medical_data.referti"
        };
        long total = 0;
        for (String table : tables) {
            total += countSafe("SELECT COUNT(*) FROM " + table);
        }
        return total;
    }

    /**
     * Conta documenti creati in un periodo (usando espressioni SQL per le date).
     */
    private long countAllDocumentsInPeriod(String fromExpr, String toExpr) {
        String[] tables = {
                "medical_data.visite_mediche", "medical_data.ricette",
                "medical_data.esenzioni", "medical_data.certificati_medici",
                "medical_data.vaccini", "medical_data.scontrini",
                "medical_data.invalidita", "medical_data.referti"
        };
        long total = 0;
        for (String table : tables) {
            total += countSafe(String.format(
                    "SELECT COUNT(*) FROM %s WHERE created_at >= %s AND created_at < %s",
                    table, fromExpr, toExpr
            ));
        }
        return total;
    }

    /**
     * Calcola il trend percentuale tra periodo corrente e precedente.
     */
    private double computeTrend(String currentPeriodSql, String previousPeriodSql) {
        long current = countSafe(currentPeriodSql);
        long previous = countSafe(previousPeriodSql);
        return calculateTrendPercent(current, previous);
    }

    private double calculateTrendPercent(long current, long previous) {
        if (previous == 0) return current > 0 ? 100.0 : 0.0;
        return Math.round(((double) (current - previous) / previous) * 1000.0) / 10.0;
    }

    /**
     * Riempie i conteggi giornalieri nella mappa.
     * Accumula (+=) per supportare più tabelle sullo stesso campo.
     */
    private void fillDailyCount(Map<String, ChartDataPointDto> dayMap, String field, String sql, LocalDate startDate) {
        try {
            jdbcTemplate.query(sql, new Object[]{java.sql.Date.valueOf(startDate)}, (rs) -> {
                String day = rs.getDate("day").toLocalDate().format(ISO_DATE);
                long cnt = rs.getLong("cnt");

                ChartDataPointDto point = dayMap.get(day);
                if (point != null) {
                    switch (field) {
                        case "users" -> point.setUsers(point.getUsers() + cnt);
                        case "documents" -> point.setDocuments(point.getDocuments() + cnt);
                        case "messages" -> point.setMessages(point.getMessages() + cnt);
                    }
                }
            });
        } catch (Exception e) {
            log.warn("Errore aggregazione giornaliera per {}: {}", field, e.getMessage());
        }
    }

    /**
     * Calcola l'uptime come percentuale basata sul tempo di avvio della JVM.
     * Approssimazione: uptime JVM / tempo dal primo deploy (30 giorni).
     */
    private double calculateUptime() {
        try {
            long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
            long thirtyDaysMs = 30L * 24 * 60 * 60 * 1000;
            double uptime = Math.min(((double) uptimeMs / thirtyDaysMs) * 100.0, 99.99);
            return Math.round(uptime * 10.0) / 10.0;
        } catch (Exception e) {
            return 99.9;
        }
    }

    /**
     * Stima la dimensione del database in GB.
     */
    private double getDatabaseSizeGB() {
        try {
            Long bytes = jdbcTemplate.queryForObject(
                    "SELECT pg_database_size(current_database())", Long.class
            );
            if (bytes != null) {
                return Math.round((bytes / (1024.0 * 1024.0 * 1024.0)) * 100.0) / 100.0;
            }
        } catch (Exception e) {
            log.warn("Errore calcolo dimensione DB: {}", e.getMessage());
        }
        return 0.0;
    }

    /**
     * Mappa action dell'audit_log al tipo di attività per il frontend.
     */
    private String mapActionToActivityType(String action) {
        if (action == null) return "alert";
        String upper = action.toUpperCase();

        if (upper.contains("REGISTRATION") || upper.contains("LOGIN") || upper.contains("USER")) {
            return "user_registration";
        }
        if (upper.contains("UPLOAD") || upper.contains("DOCUMENT") || upper.contains("CREATE")) {
            return "document_upload";
        }
        if (upper.contains("CERTIFICAT")) {
            return "certificate_issued";
        }
        if (upper.contains("MESSAG") || upper.contains("RISPOSTA")) {
            return "message";
        }
        return "alert";
    }

    /**
     * Costruisce una descrizione leggibile dell'attività.
     */
    private String buildActivityDescription(String action, String username, String details) {
        if (action == null) return details != null ? details : "Attività sconosciuta";

        String user = username != null ? username : "utente";
        String upper = action.toUpperCase();

        if (upper.contains("LOGIN")) return user + " ha effettuato l'accesso";
        if (upper.contains("REGISTRATION")) return "Nuovo utente registrato: " + user;
        if (upper.contains("ENABLE_USER")) return "Utente abilitato da " + user;
        if (upper.contains("DISABLE_USER")) return "Utente disabilitato da " + user;
        if (upper.contains("DELETE_USER")) return "Utente eliminato da " + user;
        if (upper.contains("RESTORE_USER")) return "Utente ripristinato da " + user;
        if (upper.contains("ANONYMIZE")) return "Utente anonimizzato (GDPR) da " + user;
        if (upper.contains("CHANGE_ROLE")) return "Ruolo utente modificato da " + user;
        if (upper.contains("RESET_PASSWORD")) return "Reset password avviato da " + user;
        if (upper.contains("VERIFY_EMAIL")) return "Email verificata da " + user;
        if (upper.contains("CERTIFICAT")) return "Certificato medico gestito da " + user;
        if (upper.contains("UPLOAD")) return "Documento caricato da " + user;
        if (upper.contains("MESSAG") || upper.contains("RISPOSTA")) return "Messaggio inviato da " + user;
        if (upper.contains("NOTIFICATION") || upper.contains("PUSH")) return "Notifica inviata da " + user;

        // Fallback: dettagli o azione raw
        return details != null ? details : action + " - " + user;
    }
}