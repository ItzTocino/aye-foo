package org.communityapp.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.time.Clock;
import java.util.Map;
import org.communityapp.dao.RegistrationDao;
import org.communityapp.service.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Public details for a published, upcoming cleanup. */
@WebServlet("/events/*")
public final class EventDetailServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(EventDetailServlet.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getPathInfo();
        if (path == null || !path.matches("/[a-z0-9]+(?:-[a-z0-9]+)*") || path.length() > 181) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            EventService service = (EventService) getServletContext().getAttribute(EventService.class.getName());
            Map<String, String> event = service.detail(path.substring(1));
            if (event == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.setAttribute("event", event);
            var session = request.getSession(false);
            if (session != null) {
                Object flash = session.getAttribute("registrationStatus");
                session.removeAttribute("registrationStatus");
                request.setAttribute("registrationStatus", flash);
            }
            request.setAttribute("eventSlug", path.substring(1));
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            var formSession = request.getSession();
            synchronized (formSession) {
                RegistrationTokens tokens = (RegistrationTokens) formSession.getAttribute("registrationTokens");
                if (tokens == null) {
                    tokens = new RegistrationTokens();
                    formSession.setAttribute("registrationTokens", tokens);
                }
                tokens.issue(token, path.substring(1), Clock.systemUTC().instant());
            }
            request.setAttribute("registrationToken", token);
            request.getRequestDispatcher("/WEB-INF/jsp/event-detail.jsp").forward(request, response);
        } catch (java.sql.SQLException unavailable) {
            log.warn("event detail database unavailable: SQLState={} vendorCode={}",
                    unavailable.getSQLState(), unavailable.getErrorCode());
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getPathInfo();
        if (path == null || !path.matches("/[a-z0-9]+(?:-[a-z0-9]+)*") || path.length() > 181) {
            response.sendError(404);
            return;
        }
        request.setCharacterEncoding("UTF-8");
        var session = request.getSession(false);
        String supplied = request.getParameter("csrf");
        RegistrationTokens tokens = session == null ? null
                : (RegistrationTokens) session.getAttribute("registrationTokens");
        Instant renderedAt = tokens == null ? null : tokens.consume(supplied, path.substring(1));
        Instant now = Clock.systemUTC().instant();
        if (renderedAt == null || Duration.between(renderedAt, now).compareTo(Duration.ofSeconds(2)) < 0
                || Duration.between(renderedAt, now).compareTo(Duration.ofHours(2)) > 0
                || !blank(request.getParameter("website"))) {
            response.sendError(400, "Form expired or invalid. Reload this event and try again.");
            return;
        }
        String first = trimmed(request, "firstName");
        String last = trimmed(request, "lastName");
        String email = trimmed(request, "email").toLowerCase(java.util.Locale.ROOT);
        String phone = trimmed(request, "phone");
        String city = trimmed(request, "city");
        String state = trimmed(request, "state").toUpperCase(java.util.Locale.ROOT);
        boolean valid = length(first, 1, 80) && length(last, 1, 80)
                && length(email, 3, 254) && email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")
                && length(phone, 0, 32) && length(city, 0, 100)
                && (state.isEmpty() || state.matches("[A-Z]{2}"))
                && "yes".equals(request.getParameter("consent"));
        if (!valid) {
            response.sendError(400, "Check your contact details and consent, then reload to retry.");
            return;
        }
        try {
            RegistrationDao dao = (RegistrationDao) getServletContext().getAttribute(RegistrationDao.class.getName());
            if (!dao.allowAttempt(request.getRemoteAddr(), email, now)) {
                response.sendError(429, "Too many signup attempts. Please try again later.");
                return;
            }
            RegistrationDao.Result result = dao.register(path.substring(1),
                    new RegistrationDao.Applicant(first, last, email, emptyToNull(phone),
                            emptyToNull(city), emptyToNull(state)), now);
            if (result == RegistrationDao.Result.UNAVAILABLE) {
                response.sendError(404);
                return;
            }
            String outcome = switch (result) {
                case REGISTERED -> "registered";
                case WAITLISTED -> "waitlisted";
                case DUPLICATE -> "duplicate";
                default -> throw new IllegalStateException("unexpected result");
            };
            session.setAttribute("registrationStatus", outcome);
            response.sendRedirect(request.getContextPath() + "/events/" + path.substring(1));
        } catch (java.sql.SQLException unavailable) {
            log.warn("event registration database unavailable: SQLState={} vendorCode={}",
                    unavailable.getSQLState(), unavailable.getErrorCode());
            response.sendError(503, "Signups are temporarily unavailable. Please try again later.");
        }
    }

    private static String trimmed(HttpServletRequest request, String key) {
        String value = request.getParameter(key);
        return value == null ? "" : value.strip();
    }

    private static boolean length(String value, int min, int max) {
        return value.length() >= min && value.length() <= max;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }
}
