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
 * Filtro globale per Correlation ID — Web Utility Service.
 * Vedi CorrelationIdFilter nel auth_service per documentazione completa.
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
            String correlationId = request.getHeader(HEADER_CORRELATION_ID);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }

            String clientSource = request.getHeader(HEADER_CLIENT_SOURCE);
            if (clientSource == null || clientSource.isBlank()) {
                clientSource = "unknown";
            }

            MDC.put(MDC_CORRELATION_ID, correlationId);
            MDC.put(MDC_CLIENT_SOURCE, clientSource);
            response.setHeader(HEADER_CORRELATION_ID, correlationId);

            if (log.isDebugEnabled()) {
                log.debug("[{}] {} {} (source: {})",
                        correlationId.substring(0, Math.min(8, correlationId.length())),
                        request.getMethod(),
                        request.getRequestURI(),
                        clientSource);
            }

            filterChain.doFilter(request, response);

        } finally {
            MDC.remove(MDC_CORRELATION_ID);
            MDC.remove(MDC_CLIENT_SOURCE);
        }
    }

    public static String getCurrentCorrelationId() {
        String id = MDC.get(MDC_CORRELATION_ID);
        return id != null ? id : "N/A";
    }
}