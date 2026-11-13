package net.officefloor.hq.app.asof;

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
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A read of a sheet ({@code GET /api/sheets/<id>...}) with {@code ?asOf=<yyyy-MM-dd>} reads the whole sheet as it
 * stood at the end of the last day on or before that date ({@link SheetDayRepository#schemaAsOf}), with the date
 * standing in for the sheet's today: every endpoint reads it unchanged, as every connection the request takes is
 * pointed at that sheet. Only reads are viewed so; a change is never made as of a date.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AsOfFilter extends OncePerRequestFilter {

    private static final Pattern SHEET_READ = Pattern.compile("^/api/sheets/([^/]+)(/.*)?$");

    private final SheetDayRepository days;

    public AsOfFilter(SheetDayRepository days) {
        this.days = days;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String asOf = request.getParameter("asOf");
        Matcher read = SHEET_READ.matcher(request.getRequestURI());
        if (!"GET".equals(request.getMethod()) || asOf == null || asOf.isBlank() || !read.matches()) {
            AsOfView.set(null);
            chain.doFilter(request, response);
            return;
        }
        LocalDate date;
        try {
            date = LocalDate.parse(asOf.trim());
        } catch (DateTimeParseException e) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "Not a date: " + asOf);
            return;
        }
        String sheetId = URLDecoder.decode(read.group(1), StandardCharsets.UTF_8);
        AsOfView.set(null);
        String schema = days.schemaAsOf(sheetId, date);
        AsOfView.set(new AsOfView(schema, date));
        try {
            chain.doFilter(request, response);
        } finally {
            AsOfView.set(null);
        }
    }
}
