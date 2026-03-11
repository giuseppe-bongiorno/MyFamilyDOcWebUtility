package it.myfamilydoc.webutility.aop;

import java.io.IOException;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

@Component
public class CorrelationIdInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        
        String correlationId = MDC.get("correlationId"); // recupera dal contesto
        if (correlationId != null) {
            request.getHeaders().add("X-Correlation-ID", correlationId);
        }

        return execution.execute(request, body);
    }
}
