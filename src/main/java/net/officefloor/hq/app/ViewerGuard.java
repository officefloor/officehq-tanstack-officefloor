package net.officefloor.hq.app;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.officefloor.hq.app.asof.AsOfView;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.Roles;
import net.officefloor.hq.app.sheet.SheetArchive;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A viewer sees every screen and changes nothing: every write to a sheet (POST or PUT) by a user the sheet makes
 * a viewer is refused with a 400 before any step runs, so nothing is written nor audited. The bulk actions are let
 * through: they refuse each selected item themselves (the claim status rules refuse a viewer), reporting 0 done; so is
 * a viewer's bulk grade and bulk retire, which change no one, and a viewer's generate of planned claims or of periods,
 * which makes none.
 * An archived sheet refuses every write the same way, whoever makes it. So does a screen showing the sheet as of a
 * date (?asOf=), bulk actions included: nothing changes while the as-of date is set.
 * A user's own locale and people-list columns are not a change to the sheet: anyone may choose theirs, whatever the sheet.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ViewerGuard extends OncePerRequestFilter {

    private static final Pattern SHEET_WRITE = Pattern.compile("^/api/sheets/([^/]+)(/.*)?$");

    /** The writes that report per-item counts, so a viewer's press goes through with every item refused. */
    private static final Set<String> BULK = Set.of("/claims/bulk-open", "/claims/bulk-settle", "/claims/bulk-void");

    /** The writes that report a count, so a viewer's press goes through and changes no one (still refused when archived). */
    private static final Set<String> VIEWER_COUNTED = Set.of("/people/bulk-grade", "/people/bulk-retire",
            "/plans/generate", "/periods/generate");

    /** The user's own locale: how the screen shows the sheet to them, never a change to it. */
    private static final String LOCALE = "/locale";

    /** The user's own people-list columns: which cells their screen shows, never a change to the sheet. */
    private static final String COLUMNS = "/columns";

    private final Roles roles;
    private final SheetArchive archive;

    public ViewerGuard(Roles roles, SheetArchive archive) {
        this.roles = roles;
        this.archive = archive;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String method = request.getMethod();
        String user = request.getParameter("user");
        Matcher own = SHEET_WRITE.matcher(request.getRequestURI().substring(request.getContextPath().length()));
        if (own.matches() && (LOCALE.equals(own.group(2)) || COLUMNS.equals(own.group(2)))) {
            chain.doFilter(request, response);
            return;
        }
        if (("POST".equals(method) || "PUT".equals(method)) && request.getParameter(AsOfView.PARAM) != null
                && SHEET_WRITE.matcher(request.getRequestURI().substring(request.getContextPath().length())).matches()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Nothing changes while the sheet is shown as of a date");
            return;
        }
        if (("POST".equals(method) || "PUT".equals(method)) && user != null) {
            Matcher m = SHEET_WRITE.matcher(request.getRequestURI().substring(request.getContextPath().length()));
            if (m.matches() && !BULK.contains(m.group(2) == null ? "" : m.group(2))) {
                String sheet = URLDecoder.decode(m.group(1), StandardCharsets.UTF_8);
                if (!VIEWER_COUNTED.contains(m.group(2) == null ? "" : m.group(2)) && Role.VIEWER.equals(roles.of(sheet, user))) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "A viewer cannot change the sheet");
                    return;
                }
                if (archive.archived(sheet)) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "An archived sheet cannot change");
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }
}
