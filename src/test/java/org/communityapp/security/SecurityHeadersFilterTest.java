package org.communityapp.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SecurityHeadersFilterTest {

    @Test
    void httpsRequestSetsHstsAndPassesThrough() throws Exception {
        Map<String, String> headers = new HashMap<>();
        boolean[] called = {false};
        FilterChain chain = (request, response) -> called[0] = true;

        new SecurityHeadersFilter().doFilter(request(true), response(headers), chain);

        assertTrue(called[0]);
        assertEquals("max-age=31536000", headers.get("Strict-Transport-Security"));
        assertEquals("nosniff", headers.get("X-Content-Type-Options"));
        assertTrue(headers.get("Content-Security-Policy").contains("frame-ancestors 'none'"));
    }

    @Test
    void httpRequestDoesNotAnnounceHsts() throws Exception {
        Map<String, String> headers = new HashMap<>();

        new SecurityHeadersFilter().doFilter(request(false), response(headers), (request, response) -> { });

        assertFalse(headers.containsKey("Strict-Transport-Security"));
        assertEquals("DENY", headers.get("X-Frame-Options"));
    }

    private static HttpServletRequest request(boolean secure) {
        return (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),
                new Class<?>[] {HttpServletRequest.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("isSecure")) {
                        return secure;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static HttpServletResponse response(Map<String, String> headers) {
        return (HttpServletResponse) Proxy.newProxyInstance(HttpServletResponse.class.getClassLoader(),
                new Class<?>[] {HttpServletResponse.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("setHeader")) {
                        headers.put((String) args[0], (String) args[1]);
                        return null;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}
