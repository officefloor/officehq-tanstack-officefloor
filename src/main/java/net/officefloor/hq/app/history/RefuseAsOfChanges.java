package net.officefloor.hq.app.history;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * While the signed-in user's screen shows the sheet as of a date (every request carries {@code asof}), every change is
 * refused before it reaches its endpoint, so it writes nothing to the store or the audit file. Reads, signing in and
 * loading a sheet go through.
 */
@Component
public class RefuseAsOfChanges extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String asof = request.getParameter("asof");
        return "GET".equals(request.getMethod()) || asof == null || asof.isBlank() || !path.startsWith("/api/")
                || path.equals("/api/signin") || ("PUT".equals(request.getMethod()) && path.matches("/api/sheets/[^/]+"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"The sheet is shown as of a date: clear it to make changes\"}");
    }
}
