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
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.SheetClock;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A screen showing the sheet as of a date (?asOf=yyyy-MM-dd on its reads): each read of the sheet runs in a
 * transaction that first puts the sheet as it stood at the end of that date (see {@link SheetSnapshots#show}) and is
 * always rolled back, so every figure the endpoints make is made from that state, with the date for the sheet's
 * today, and nothing is ever written. The sheet's parents whose rates it takes (see Rates) are put as they stood at
 * the end of that date too, bar a parent whose today was never set: it has no dated history, so it is taken as it
 * stands now — unless the sheet itself had nothing then (the date is before its history), when no parent is taken.
 * So too the sheet it was cloned from, and its parents and source in turn.
 * Every change sent while the as-of date is set is refused (see ViewerGuard).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AsOfView extends OncePerRequestFilter {

    public static final String PARAM = "asOf";

    private static final Pattern SHEET = Pattern.compile("^/api/sheets/([^/]+)(/.*)?$");

    private final SheetSnapshots snapshots;
    private final Settings settings;
    private final SheetClock clock;
    private final Rates rates;
    private final TransactionTemplate transaction;

    public AsOfView(SheetSnapshots snapshots, Settings settings, SheetClock clock, Rates rates,
            PlatformTransactionManager transactions) {
        this.snapshots = snapshots;
        this.settings = settings;
        this.clock = clock;
        this.rates = rates;
        this.transaction = new TransactionTemplate(transactions);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String raw = request.getParameter(PARAM);
        Matcher m = SHEET.matcher(request.getRequestURI().substring(request.getContextPath().length()));
        if (raw == null || raw.isBlank() || !"GET".equals(request.getMethod()) || !m.matches()) {
            chain.doFilter(request, response);
            return;
        }
        LocalDate date;
        try {
            date = LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Not a valid as-of date: " + raw);
            return;
        }
        String sheet = URLDecoder.decode(m.group(1), StandardCharsets.UTF_8);
        try {
            transaction.executeWithoutResult((status) -> {
                status.setRollbackOnly();
                boolean had = snapshots.show(sheet, date);
                Set<String> shown = new HashSet<>(Set.of(sheet));
                for (String from = sheet; !from.isEmpty(); ) {
                    String parent = settings.parent(from);
                    for (int level = 1; level <= Rate.LEVELS && !parent.isEmpty() && shown.add(parent); level++) {
                        if (clock.isSet(parent) || !had) {
                            snapshots.show(parent, date);
                        }
                        parent = settings.parent(parent);
                    }
                    from = rates.source(from);
                    if (!from.isEmpty() && shown.add(from)) {
                        if (clock.isSet(from) || !had) {
                            snapshots.show(from, date);
                        }
                    } else {
                        from = "";
                    }
                }
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
