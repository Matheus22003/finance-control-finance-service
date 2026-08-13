package com.financecontrol.finance.observability;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Correlation-ID";
    public static final String REQUEST_ATTRIBUTE_NAME = "financeControl.correlationId";
    private static final Logger LOGGER = LoggerFactory.getLogger(CorrelationIdFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        var correlationId = resolveCorrelationId(request);
        request.setAttribute(REQUEST_ATTRIBUTE_NAME, correlationId);
        response.setHeader(HEADER_NAME, correlationId);

        var startedAt = System.nanoTime();
        try (var ignored = MDC.putCloseable("correlationId", correlationId)) {
            LOGGER.atInfo()
                    .addKeyValue("requestMethod", request.getMethod())
                    .addKeyValue("requestPath", request.getRequestURI())
                    .log("HTTP request started");

            try {
                filterChain.doFilter(request, response);
            } finally {
                var elapsedMilliseconds = (System.nanoTime() - startedAt) / 1_000_000.0;
                LOGGER.atInfo()
                        .addKeyValue("requestMethod", request.getMethod())
                        .addKeyValue("requestPath", request.getRequestURI())
                        .addKeyValue("statusCode", response.getStatus())
                        .addKeyValue("elapsedMilliseconds", elapsedMilliseconds)
                        .log("HTTP request completed");
            }
        }
    }

    public static String getCorrelationId(HttpServletRequest request) {
        var value = request.getAttribute(REQUEST_ATTRIBUTE_NAME);
        return value instanceof String correlationId ? correlationId : null;
    }

    private static String resolveCorrelationId(HttpServletRequest request) {
        var headerValues = Collections.list(request.getHeaders(HEADER_NAME));
        if (headerValues.size() == 1) {
            var candidate = headerValues.getFirst();
            try {
                var parsed = UUID.fromString(candidate);
                if (parsed.toString().equalsIgnoreCase(candidate)) {
                    return parsed.toString();
                }
            } catch (IllegalArgumentException ignored) {
                // A fresh identifier is generated below.
            }
        }

        return UUID.randomUUID().toString();
    }
}
