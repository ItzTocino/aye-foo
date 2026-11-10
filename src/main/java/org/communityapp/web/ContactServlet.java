package org.communityapp.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Renders the Contact page.
 */
@WebServlet("/contact")
public final class ContactServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("pageTitle", "Contact");
        request.setAttribute("description", "Questions, partnerships, and press for SaveOurWorld.");
        request.setAttribute("activeNav", "contact");
        request.getRequestDispatcher("/WEB-INF/jsp/contact.jsp").forward(request, response);
    }
}
