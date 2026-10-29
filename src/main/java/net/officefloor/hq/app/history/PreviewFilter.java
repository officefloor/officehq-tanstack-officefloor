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
 * A screen previewing a date names it on every request ({@code X-Preview: yyyy-mm-dd}). A read is answered over the
 * sheet's current records as if its today were that date (see {@link Preview}); a change ignores it and uses the
 * sheet's real today, so nothing is stored or recorded for the preview.
 */
@Component
public class PreviewFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Preview";

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/") || !"GET".equals(request.getMethod());
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
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setContentType("text/plain;charset=UTF-8");
            response.getOutputStream().write("The preview date is not a date (yyyy-mm-dd).".getBytes(StandardCharsets.UTF_8));
            return;
        }
        LocalDate was = Preview.set(date);
        try {
            chain.doFilter(request, response);
        } finally {
            Preview.set(was);
        }
    }
}
