package net.officefloor.hq.app.history;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A screen shown as of a date names it on every request ({@code X-As-Of: yyyy-mm-dd}). A read is answered from the
 * store as it stood at the end of that date (see {@link AsOf}); a change is refused (422) before anything is read,
 * stored or recorded. A request without the header is served as it is now.
 */
@Component
public class AsOfFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-As-Of";

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String raw = request.getHeader(HEADER);
        if (raw == null || raw.isBlank()) {
            chain.doFilter(request, response);
            return;
        }
        LocalDate date;
        try {
            date = LocalDate.parse(raw.trim(), ISO);
        } catch (DateTimeParseException e) {
            refuse(response, HttpStatus.BAD_REQUEST, "The as-of date is not a date (yyyy-mm-dd).");
            return;
        }
        if (!"GET".equals(request.getMethod())) {
            refuse(response, HttpStatus.UNPROCESSABLE_ENTITY,
                    "The sheet is shown as of " + date + ": clear the as-of date to change anything.");
            return;
        }
        LocalDate was = AsOf.set(date);
        try {
            chain.doFilter(request, response);
        } finally {
            AsOf.set(was);
        }
    }

    private static void refuse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("text/plain;charset=UTF-8");
        response.getOutputStream().write(message.getBytes(StandardCharsets.UTF_8));
    }
}
