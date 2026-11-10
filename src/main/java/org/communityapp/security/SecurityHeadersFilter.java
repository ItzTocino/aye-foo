package org.communityapp.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Adds the browser security headers every response needs.
 */
@WebFilter("/*")
public final class SecurityHeadersFilter implements Filter {

    private static final String CONTENT_SECURITY_POLICY = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self'",
            "img-src 'self' data:",
            "connect-src 'self'",
            "frame-ancestors 'none'",
            "base-uri 'none'");
    private static final String STRICT_TRANSPORT_SECURITY = "max-age=31536000";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse http = (HttpServletResponse) response;
        // Do not emit HSTS for plain HTTP (including local development). TLS termination
        // must make request.isSecure() accurate through trusted container configuration.
        if (((HttpServletRequest) request).isSecure()) {
            http.setHeader("Strict-Transport-Security", STRICT_TRANSPORT_SECURITY);
        }
        http.setHeader("Content-Security-Policy", CONTENT_SECURITY_POLICY);
        http.setHeader("X-Content-Type-Options", "nosniff");
        http.setHeader("X-Frame-Options", "DENY");
        http.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        http.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
        chain.doFilter(request, response);
    }
}
