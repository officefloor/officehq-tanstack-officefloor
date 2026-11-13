package net.officefloor.hq.app.sheet;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Applies the preview date a screen sends with each read ({@value #HEADER}, yyyy-MM-dd): while the read is
 * answered, {@link SheetStore#today(String)} gives that date in place of the sheet's today, so every figure is
 * worked out as if the sheet's today were that date, over the sheet's current records. The preview belongs to
 * the screen; only a read takes it, so every change still uses the sheet's real today.
 */
@Component
public class PreviewFilter extends OncePerRequestFilter {

    /** The header a screen with a preview date set sends with each read. */
    public static final String HEADER = "X-Preview";

    private static final ThreadLocal<LocalDate> PREVIEW = new ThreadLocal<>();

    /** The preview date of the read being answered, null when none. */
    static LocalDate current() {
        return PREVIEW.get();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/") || !"GET".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        LocalDate date = parse(request.getHeader(HEADER));
        if (date == null) {
            chain.doFilter(request, response);
            return;
        }
        PREVIEW.set(date);
        try {
            chain.doFilter(request, response);
        } finally {
            PREVIEW.remove();
        }
    }

    private static LocalDate parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
