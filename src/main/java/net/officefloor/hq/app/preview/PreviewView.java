package net.officefloor.hq.app.preview;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.officefloor.hq.app.asof.AsOfView;
import net.officefloor.hq.app.sheet.SheetClock;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A screen previewing the sheet at a date (?preview=yyyy-MM-dd on its reads): each read of the sheet's figures runs
 * in a transaction that first puts the date in for the sheet's today, over its current records, and is always rolled
 * back, so every figure the endpoints make is made as if that date were its today, and nothing is written. Only the
 * today is moved: nothing a change of today does (snapshots, scores, auto-closing) happens. The sheet itself
 * (GET /api/sheets/{id}) still answers its real today, which every change and every default date uses. Writes never
 * carry the preview (the screen sends it on reads only). An as-of date, when set too, takes precedence.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class PreviewView extends OncePerRequestFilter {

    public static final String PARAM = "preview";

    private static final Pattern SHEET_READ = Pattern.compile("^/api/sheets/([^/]+)/.+$");

    private final SheetClock clock;
    private final TransactionTemplate transaction;

    public PreviewView(SheetClock clock, PlatformTransactionManager transactions) {
        this.clock = clock;
        this.transaction = new TransactionTemplate(transactions);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String raw = request.getParameter(PARAM);
        String asOf = request.getParameter(AsOfView.PARAM);
        Matcher m = SHEET_READ.matcher(request.getRequestURI().substring(request.getContextPath().length()));
        if (raw == null || raw.isBlank() || (asOf != null && !asOf.isBlank()) || !"GET".equals(request.getMethod())
                || !m.matches()) {
            chain.doFilter(request, response);
            return;
        }
        LocalDate date;
        try {
            date = LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Not a valid preview date: " + raw);
            return;
        }
        String sheet = URLDecoder.decode(m.group(1), StandardCharsets.UTF_8);
        try {
            transaction.executeWithoutResult((status) -> {
                status.setRollbackOnly();
                clock.setToday(sheet, date);
                try {
                    chain.doFilter(request, response);
                } catch (IOException | ServletException e) {
                    throw new IllegalStateException(e);
                }
            });
        } catch (IllegalStateException e) {
            if (e.getCause() instanceof IOException io) {
                throw io;
            }
            if (e.getCause() instanceof ServletException se) {
                throw se;
            }
            throw e;
        }
    }
}
