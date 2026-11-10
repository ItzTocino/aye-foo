package org.communityapp.web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Gives every request a correlation id, then logs one line when it finishes.
 */
@WebFilter("/*")
public final class RequestLogFilter implements Filter {

    public static final String CORRELATION_ID_ATTRIBUTE = "correlationId";

    private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String correlationId = UUID.randomUUID().toString();
        request.setAttribute(CORRELATION_ID_ATTRIBUTE, correlationId);
        MDC.put(CORRELATION_ID_ATTRIBUTE, correlationId);
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        long startedAt = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            log.info("{} {} {} {}ms", httpRequest.getMethod(), httpRequest.getRequestURI(),
                    httpResponse.getStatus(), durationMs);
            MDC.remove(CORRELATION_ID_ATTRIBUTE);
        }
    }
}
