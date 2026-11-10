package org.communityapp.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Renders the About us page.
 */
@WebServlet("/about")
public final class AboutServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("pageTitle", "About us");
        request.setAttribute("description", "Who SaveOurWorld is and why we work on the Chesapeake Bay.");
        request.setAttribute("activeNav", "about");
        request.getRequestDispatcher("/WEB-INF/jsp/about.jsp").forward(request, response);
    }
}
