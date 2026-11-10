package org.communityapp.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.communityapp.service.ContentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders the initiatives page.
 */
@WebServlet("/initiatives")
public final class InitiativesServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(InitiativesServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("pageTitle", "Chesapeake Bay initiatives");
        request.setAttribute("description", "Shoreline and river cleanups, conservation, and education across the Chesapeake watershed.");
        request.setAttribute("activeNav", "initiatives");
        try {
            ContentService service = (ContentService) getServletContext()
                    .getAttribute(ContentService.class.getName());
            ContentService.Content content = service.publicContent();
            request.setAttribute("initiatives", content.initiatives());
            request.setAttribute("partners", content.partners());
        } catch (java.sql.SQLException unavailable) {
            log.warn("public content database unavailable: SQLState={} vendorCode={}",
                    unavailable.getSQLState(), unavailable.getErrorCode());
            request.setAttribute("contentUnavailable", true);
        }
        request.getRequestDispatcher("/WEB-INF/jsp/initiatives.jsp").forward(request, response);
    }
}
