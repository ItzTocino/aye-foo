package org.communityapp.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.communityapp.service.ContentService;
import org.communityapp.service.ImpactService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders the front page.
 */
@WebServlet("")
public final class HomeServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(HomeServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("pageTitle", "Cleanups on the Chesapeake Bay");
        request.setAttribute("description", "SaveOurWorld runs shoreline and river cleanups around the Chesapeake Bay.");
        request.setAttribute("activeNav", "home");
        boolean databaseConnectionFailed = false;
        try {
            ContentService service = (ContentService) getServletContext()
                    .getAttribute(ContentService.class.getName());
            request.setAttribute("partners", service.publicContent().partners());
        } catch (java.sql.SQLException unavailable) {
            log.warn("home partner data unavailable: SQLState={} vendorCode={}",
                    unavailable.getSQLState(), unavailable.getErrorCode());
            request.setAttribute("partnersUnavailable", true);
            databaseConnectionFailed = unavailable.getSQLState() != null
                    && unavailable.getSQLState().startsWith("08");
        }
        if (databaseConnectionFailed) {
            // The same pool just failed to establish a connection. Avoid a second
            // acquisition wait; the next request attempts a fresh read normally.
            request.setAttribute("impactUnavailable", true);
        } else {
            try {
                ImpactService service = (ImpactService) getServletContext()
                        .getAttribute(ImpactService.class.getName());
                request.setAttribute("featuredImpact", service.featured(service.metrics()));
            } catch (java.sql.SQLException unavailable) {
                log.warn("home impact data unavailable: SQLState={} vendorCode={}",
                        unavailable.getSQLState(), unavailable.getErrorCode());
                request.setAttribute("impactUnavailable", true);
            }
        }
        request.getRequestDispatcher("/WEB-INF/jsp/home.jsp").forward(request, response);
    }
}
