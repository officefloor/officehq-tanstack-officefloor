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
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.Roles;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A viewer sees every screen and changes nothing: every write to a sheet (POST or PUT) by a user the sheet makes
 * a viewer is refused with a 400 before any step runs, so nothing is written nor audited. The bulk actions are let
 * through: they refuse each selected item themselves (the claim status rules refuse a viewer), reporting 0 done.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ViewerGuard extends OncePerRequestFilter {

    private static final Pattern SHEET_WRITE = Pattern.compile("^/api/sheets/([^/]+)(/.*)?$");

    /** The writes that report per-item counts, so a viewer's press goes through with every item refused. */
    private static final Set<String> BULK = Set.of("/claims/bulk-open", "/claims/bulk-settle");

    private final Roles roles;

    public ViewerGuard(Roles roles) {
        this.roles = roles;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String method = request.getMethod();
        String user = request.getParameter("user");
        if (("POST".equals(method) || "PUT".equals(method)) && user != null) {
            Matcher m = SHEET_WRITE.matcher(request.getRequestURI().substring(request.getContextPath().length()));
            if (m.matches() && !BULK.contains(m.group(2) == null ? "" : m.group(2))) {
                String sheet = URLDecoder.decode(m.group(1), StandardCharsets.UTF_8);
                if (Role.VIEWER.equals(roles.of(sheet, user))) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "A viewer cannot change the sheet");
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }
}
