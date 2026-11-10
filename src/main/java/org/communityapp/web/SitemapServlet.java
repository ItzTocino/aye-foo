package org.communityapp.web;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Renders absolute sitemap locations for the currently configured public origin. */
@WebServlet("/sitemap.xml")
public final class SitemapServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String[] PATHS = {"/", "/about", "/initiatives", "/get-involved", "/contact"};

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String origin = System.getenv("SITE_ORIGIN");
        if (origin == null || !origin.matches("https://[A-Za-z0-9.-]+(?::[0-9]{2,5})?")) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "SITE_ORIGIN must be configured for the sitemap");
            return;
        }
        response.setContentType("application/xml;charset=UTF-8");
        response.getWriter().write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        for (String path : PATHS) {
            response.getWriter().write("  <url><loc>" + origin + request.getContextPath()
                    + path + "</loc></url>\n");
        }
        response.getWriter().write("</urlset>\n");
    }
}
