package org.communityapp.web;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.communityapp.service.HealthService;

/**
 * Reports application health as JSON for the reverse proxy and uptime checks.
 */
@WebServlet("/healthz")
public final class HealthServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private transient HealthService healthService;

    @Override
    public void init() {
        Object service = getServletContext().getAttribute(HealthService.class.getName());
        if (service == null) {
            throw new IllegalStateException("HealthService is not wired");
        }
        healthService = (HealthService) service;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        boolean ready = healthService.isReady();
        response.setStatus(ready ? HttpServletResponse.SC_OK : HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(ready ? "{\"status\":\"ok\"}" : "{\"status\":\"degraded\"}");
    }
}
