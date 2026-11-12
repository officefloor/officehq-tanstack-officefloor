package net.officefloor.hq.app.sheet;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Refuses every change made from a screen showing a sheet as of a date: a request to {@code /api/} other than
 * a read, sent with the {@value #HEADER} header (the date the screen shows), answers 400 with the reason, and
 * reaches no endpoint, so it writes nothing to the store or the audit file. The as-of date belongs to the
 * screen; a change sent without it goes through as usual. A user's choice of locale changes no sheet, so
 * it goes through either way.
 */
@Component
public class AsOfGuard extends OncePerRequestFilter {

    /** The header a screen showing the sheet as of a date sends with each request. */
    public static final String HEADER = "X-As-Of";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/") || request.getRequestURI().endsWith("/locale")
                || "GET".equals(request.getMethod())
                || "HEAD".equals(request.getMethod()) || "OPTIONS".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String asOf = request.getHeader(HEADER);
        if (asOf == null || asOf.isBlank()) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType("text/plain;charset=UTF-8");
        response.getOutputStream().write(("The sheet is shown as of " + asOf.trim() + ": clear the as-of date to change it")
                .getBytes(StandardCharsets.UTF_8));
    }
}
