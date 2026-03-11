package it.myfamilydoc.webutility.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filtro globale per Correlation ID.
 *
 * Intercetta TUTTE le richieste HTTP e:
 * 1. Legge X-Correlation-ID dall'header (inviato dal client Flutter)
 * 2. Se assente, ne genera uno nuovo (per chiamate da browser/Postman)
 * 3. Lo inserisce nel MDC di SLF4J -> tutti i log della request lo includono
 * 4. Lo aggiunge alla response header -> il client puo verificarlo
 * 5. Lo pulisce a fine request (evita leak tra thread)
 *
 * Funziona automaticamente con @Auditable, @Slf4j, logger.info(), ecc.
 * senza modificare nessun controller.
 *
 * Requisito: aggiungere %X{correlationId} al pattern di logback.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    public static final String HEADER_CORRELATION_ID = "X-Correlation-ID";
    public static final String HEADER_CLIENT_SOURCE = "X-Client-Source";
    public static final String MDC_CORRELATION_ID = "correlationId";
    public static final String MDC_CLIENT_SOURCE = "clientSource";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // 1. Leggi o genera Correlation ID
            String correlationId = request.getHeader(HEADER_CORRELATION_ID);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }

            // 2. Leggi Client Source
            String clientSource = request.getHeader(HEADER_CLIENT_SOURCE);
            if (clientSource == null || clientSource.isBlank()) {
                clientSource = "unknown";
            }

            // 3. Inserisci nel MDC (disponibile in tutti i log di questa request)
            MDC.put(MDC_CORRELATION_ID, correlationId);
            MDC.put(MDC_CLIENT_SOURCE, clientSource);

            // 4. Aggiungi alla response (il client puo verificarlo)
            response.setHeader(HEADER_CORRELATION_ID, correlationId);

            // 5. Log della request (opzionale, utile per debug)
            if (log.isDebugEnabled()) {
                log.debug("[{}] {} {} (source: {})",
                        correlationId.substring(0, Math.min(8, correlationId.length())),
                        request.getMethod(),
                        request.getRequestURI(),
                        clientSource);
            }

            // 6. Prosegui con la filter chain
            filterChain.doFilter(request, response);

        } finally {
            // 7. Pulisci MDC (CRITICO: evita leak tra richieste sullo stesso thread)
            MDC.remove(MDC_CORRELATION_ID);
            MDC.remove(MDC_CLIENT_SOURCE);
        }
    }

    /**
     * Metodo statico per accedere al correlation ID corrente da qualsiasi punto.
     * Utile per servizi che devono propagarlo a chiamate esterne.
     */
    public static String getCurrentCorrelationId() {
        String id = MDC.get(MDC_CORRELATION_ID);
        return id != null ? id : "N/A";
    }
}