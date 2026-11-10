package org.communityapp.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.communityapp.service.DonationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders the donation page. Checkout is added in the donations phase.
 */
@WebServlet("/donate")
public final class DonateServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(DonateServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("pageTitle", "Donate");
        request.setAttribute("description", "Support shoreline and river cleanups around the Chesapeake Bay.");
        request.setAttribute("activeNav", "donate");
        try {
            DonationService service = (DonationService) getServletContext()
                    .getAttribute(DonationService.class.getName());
            request.setAttribute("donationProgress", service.progress());
        } catch (java.sql.SQLException unavailable) {
            log.warn("donation ledger unavailable: SQLState={} vendorCode={}",
                    unavailable.getSQLState(), unavailable.getErrorCode());
            request.setAttribute("donationProgressUnavailable", true);
        }
        request.getRequestDispatcher("/WEB-INF/jsp/donate.jsp").forward(request, response);
    }
}
