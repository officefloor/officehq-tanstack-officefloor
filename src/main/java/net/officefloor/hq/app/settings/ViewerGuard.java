package net.officefloor.hq.app.settings;

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
import java.nio.charset.StandardCharsets;
import java.util.Set;
import net.officefloor.hq.app.sheet.Sheets;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** A viewer sees every screen and can change nothing: every change to a sheet (a POST or PUT under
 *  {@code /api/sheets/<id>/}) by a user who is a viewer on that sheet is refused 400 before it
 *  reaches its pipeline, so it writes nothing and audits nothing. A bulk action still goes through,
 *  each selected item refused by the permission matrix (see {@link Settings#allows}), so its counts
 *  show none done. An archived sheet is guarded the same way for every user. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ViewerGuard extends OncePerRequestFilter {

    private static final String PREFIX = "/api/sheets/";

    /** The bulk actions, by the path after the sheet id: they answer counts instead of refusing. */
    private static final Set<String> BULK = Set.of("bulk-open", "bulk-settle");

    private static final ObjectMapper JSON = new ObjectMapper();

    private final Settings settings;
    private final Sheets sheets;

    public ViewerGuard(Settings settings, Sheets sheets) {
        this.settings = settings;
        this.sheets = sheets;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        return !(method.equals("POST") || method.equals("PUT") || method.equals("DELETE"))
                || !request.getRequestURI().startsWith(PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String rest = request.getRequestURI().substring(PREFIX.length());
        int slash = rest.indexOf('/');
        if (slash <= 0) {
            chain.doFilter(request, response);
            return;
        }
        String sheetId = UriUtils.decode(rest.substring(0, slash), StandardCharsets.UTF_8);
        String action = rest.substring(slash + 1);
        byte[] body = request.getInputStream().readAllBytes();
        String user = user(body);
        if (user != null && !BULK.contains(action) && settings.role(sheetId, user) == Role.VIEWER) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("A viewer can change nothing");
            return;
        }
        if (!BULK.contains(action) && sheets.archived(sheetId)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("The sheet is archived: nothing on it can be changed");
            return;
        }
        chain.doFilter(new CachedBody(request, body), response);
    }

    private static String user(byte[] body) {
        try {
            JsonNode node = JSON.readTree(body);
            JsonNode user = node == null ? null : node.get("user");
            return user == null || !user.isString() ? null : user.asString().trim();
        } catch (RuntimeException e) {
            return null; // not JSON: left to the endpoint to refuse
        }
    }

    /** The request with its body already read, so the endpoint can read it again. */
    private static final class CachedBody extends HttpServletRequestWrapper {
        private final byte[] body;

        CachedBody(HttpServletRequest request, byte[] body) {
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
            String encoding = getCharacterEncoding();
            return new BufferedReader(new InputStreamReader(getInputStream(),
                    encoding == null ? StandardCharsets.UTF_8 : java.nio.charset.Charset.forName(encoding)));
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
