package net.officefloor.hq.app.role;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

/** Refuse every change a viewer asks for on a sheet, and every change to an archived sheet, before any endpoint
 *  reads or writes anything: each POST or PUT under a sheet (all but loading the sheet itself) whose signed-in user
 *  (the body's {@code user}) is a viewer on that sheet, or on a sheet that is archived, is answered 403 with the
 *  reason, and nothing is written or audited. A bulk open is left to its endpoint, which counts every selected claim
 *  refused for a viewer or on an archived sheet, and so is a bulk reassign or bulk re-date; so is a viewer's bulk grade or bulk retire, which changes nobody; so is a bulk settle by a viewer or on an archived sheet, which counts every selected claim refused. Every change sent from a
 *  screen showing the sheet as of a date (the X-As-Of header), a bulk one included, is answered 403 the same way. */
@Component
public class RefuseViewerChanges extends OncePerRequestFilter {

    /** A change under a sheet: /api/sheets/{sheetId}/... (loading the sheet, /api/sheets/{sheetId}, is not one). */
    private static final Pattern CHANGE = Pattern.compile("^/api/sheets/([^/]+)/.+$");

    /** A bulk open, left to the endpoint: it refuses each selected claim a viewer's role may not open (none), and
     *  answers how many it opened and refused, writing nothing. */
    private static final Pattern BULK_OPEN = Pattern.compile("^/api/sheets/[^/]+/bulk-(open|reassign|redate)$");

    /** A bulk grade or retire, left to the endpoint for a viewer: it changes nobody and answers that none was changed. */
    private static final Pattern BULK_GRADE = Pattern.compile("^/api/sheets/[^/]+/bulk-(grade|retire)$");

    /** A bulk settle, left to the endpoint for a viewer or on an archived sheet: it counts every selected claim refused. */
    private static final Pattern BULK_SETTLE = Pattern.compile("^/api/sheets/[^/]+/bulk-settle$");

    /** The header a screen showing the sheet as of a date sends every change with: the change is refused. */
    public static final String AS_OF = "X-As-Of";

    private final RoleRepository roles;
    private final SheetRepository sheets;
    private final JsonMapper json = JsonMapper.builder().build();

    public RefuseViewerChanges(RoleRepository roles, SheetRepository sheets) {
        this.roles = roles;
        this.sheets = sheets;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String method = request.getMethod();
        String path = request.getRequestURI().substring(request.getContextPath().length());
        Matcher change = CHANGE.matcher(path);
        if (method.equals("GET") || method.equals("HEAD") || method.equals("OPTIONS") || !change.matches()) {
            chain.doFilter(request, response);
            return;
        }
        String asOf = request.getHeader(AS_OF);
        if (asOf != null && !asOf.isBlank()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("The sheet is shown as of " + asOf.trim() + ": clear the as-of date to change it.");
            return;
        }
        if (BULK_OPEN.matcher(path).matches()) {
            chain.doFilter(request, response);
            return;
        }
        byte[] body = request.getInputStream().readAllBytes();
        String sheetId = URLDecoder.decode(change.group(1), StandardCharsets.UTF_8).trim();
        String user = user(body);
        if (user != null && !BULK_GRADE.matcher(path).matches() && !BULK_SETTLE.matcher(path).matches()
                && Role.VIEWER.equals(roles.of(sheetId, user))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("A viewer can change nothing on this sheet.");
            return;
        }
        if (!BULK_SETTLE.matcher(path).matches() && sheets.archived(sheetId)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("This sheet is archived: nothing on it can be changed.");
            return;
        }
        chain.doFilter(new Rereadable(request, body), response);
    }

    /** The signed-in user the body names (trimmed), or null when there is none. */
    private String user(byte[] body) {
        try {
            Object user = json.readValue(body, Map.class).get("user");
            return user instanceof String name && !name.isBlank() ? name.trim() : null;
        } catch (RuntimeException e) {
            return null; // not a JSON object: left to the endpoint to refuse
        }
    }

    /** The request with its body, already read here, readable again by the endpoint. */
    private static final class Rereadable extends HttpServletRequestWrapper {
        private final byte[] body;

        Rereadable(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream in = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return in.read();
                }

                @Override
                public int read(byte[] b, int off, int len) {
                    return in.read(b, off, len);
                }

                @Override
                public boolean isFinished() {
                    return in.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        @Override
        public int getContentLength() {
            return body.length;
        }

        @Override
        public long getContentLengthLong() {
            return body.length;
        }
    }
}
