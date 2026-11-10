package org.communityapp.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.communityapp.service.ImpactService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Public impact dashboard backed by the impact fact ledger. */
@WebServlet("/impact")
public final class ImpactServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ImpactServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("pageTitle", "Impact dashboard");
        request.setAttribute("description", "Recorded Chesapeake Bay cleanup and community impact.");
        request.setAttribute("impactCategories", List.of("cleanup", "conservation", "community"));
        try {
            ImpactService service = (ImpactService) getServletContext()
                    .getAttribute(ImpactService.class.getName());
            request.setAttribute("impactMetrics", service.metrics());
        } catch (java.sql.SQLException unavailable) {
            log.warn("impact facts unavailable: SQLState={} vendorCode={}",
                    unavailable.getSQLState(), unavailable.getErrorCode());
            request.setAttribute("impactUnavailable", true);
        }
        request.getRequestDispatcher("/WEB-INF/jsp/impact.jsp").forward(request, response);
    }
}
