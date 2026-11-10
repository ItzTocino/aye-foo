package org.communityapp.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.communityapp.service.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders the Get involved page.
 */
@WebServlet("/get-involved")
public final class GetInvolvedServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(GetInvolvedServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("pageTitle", "Get involved");
        request.setAttribute("description", "Ways to help SaveOurWorld: join a cleanup, give, or spread the word.");
        request.setAttribute("activeNav", "get-involved");
        try {
            EventService service = (EventService) getServletContext().getAttribute(EventService.class.getName());
            request.setAttribute("upcomingEvents", service.upcoming());
        } catch (java.sql.SQLException unavailable) {
            log.warn("event calendar database unavailable: SQLState={} vendorCode={}",
                    unavailable.getSQLState(), unavailable.getErrorCode());
            request.setAttribute("calendarUnavailable", true);
        }
        request.getRequestDispatcher("/WEB-INF/jsp/get-involved.jsp").forward(request, response);
    }
}
