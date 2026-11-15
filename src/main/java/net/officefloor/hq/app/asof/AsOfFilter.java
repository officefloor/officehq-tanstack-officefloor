package net.officefloor.hq.app.asof;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriUtils;

/**
 * A request about a sheet carrying {@code shown-asof=<yyyy-MM-dd>} comes from a screen showing the sheet as
 * of that date. A read (any GET, and loading the sheet) is answered from the sheet as it stood then
 * ({@link AsOfStore}), with the date as its today; every change is refused (403) before anything is
 * read or written, so nothing reaches the store or the audit file.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class AsOfFilter extends OncePerRequestFilter {

    private static final Pattern SHEET = Pattern.compile("^/api/sheets/([^/]+)(/.*)?$");

    private final AsOfStore store;

    public AsOfFilter(AsOfStore store) {
        this.store = store;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String asOf = request.getParameter("shown-asof");
        Matcher sheet = SHEET.matcher(request.getRequestURI().substring(request.getContextPath().length()));
        if (asOf == null || asOf.isBlank() || !sheet.matches()) {
            chain.doFilter(request, response);
            return;
        }
        boolean read = "GET".equals(request.getMethod())
                || ("POST".equals(request.getMethod()) && "/open".equals(sheet.group(2)));
        if (!read) {
            refuse(response, HttpStatus.FORBIDDEN, "The sheet is shown as of a date: nothing can be changed");
            return;
        }
        LocalDate date;
        try {
            date = LocalDate.parse(asOf.trim());
        } catch (DateTimeParseException e) {
            refuse(response, HttpStatus.BAD_REQUEST, "Not a date: " + asOf);
            return;
        }
        String schema = store.view(UriUtils.decode(sheet.group(1), StandardCharsets.UTF_8).trim(), date);
        AsOfView.enter(schema);
        try {
            chain.doFilter(request, response);
        } finally {
            AsOfView.leave();
            store.drop(schema);
        }
    }

    private static void refuse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(message);
    }
}
