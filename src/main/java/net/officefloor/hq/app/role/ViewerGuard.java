package net.officefloor.hq.app.role;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A viewer changes nothing: every change to a sheet (any non-GET request under {@code /api/sheets/<id>/}) made
 * by a user whose role on that sheet is viewer is refused (403) before it reaches its endpoint, so nothing is
 * stored and nothing audited. So is every change, by anyone, to a sheet a steward archived, and every change made from
 * a screen showing the sheet as of a date (it carries the {@code X-As-Of} header, see AsOf). A bulk action instead goes
 * through with nothing done: 0 done, every selected claim refused (a bulk grade: 0 people changed; a bulk retire: 0 people retired; a bulk hold: 0 people held; a generate of the plans' claims: 0 made).
 */
@Configuration
public class ViewerGuard {

    private static final Pattern SHEET_PATH = Pattern.compile("^/api/sheets/([^/]+)(/.*)?$");
    private static final ObjectMapper JSON = new ObjectMapper();

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> viewerGuardFilter(RoleRepository roles, SheetRepository sheets) {
        FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>(new Filter(roles, sheets));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    private static final class Filter extends OncePerRequestFilter {
        private final RoleRepository roles;
        private final SheetRepository sheets;

        Filter(RoleRepository roles, SheetRepository sheets) {
            this.roles = roles;
            this.sheets = sheets;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            String method = request.getMethod();
            Matcher m = SHEET_PATH.matcher(request.getRequestURI());
            if ("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method) || !m.matches()) {
                chain.doFilter(request, response);
                return;
            }
            String sheetId = URLDecoder.decode(m.group(1), StandardCharsets.UTF_8).trim();
            String rest = m.group(2) == null ? "" : m.group(2);
            byte[] body = request.getInputStream().readAllBytes();
            JsonNode json = parse(body);
            String by = json != null && json.hasNonNull("by") ? json.get("by").asText().trim() : "";
            boolean viewer = RoleRepository.VIEWER.equals(roles.roleOf(sheetId, by));
            boolean archived = sheets.archived(sheetId);
            String asOf = request.getHeader(AsOf.HEADER);
            boolean viewingAsOf = asOf != null && !asOf.isBlank();
            if (!viewer && !archived && !viewingAsOf) {
                chain.doFilter(new Replay(request, body), response);
                return;
            }
            if (rest.equals("/bulk-open") || rest.equals("/bulk-settle") || rest.equals("/bulk-void")) {
                int selected = json != null && json.get("claimIds") != null && json.get("claimIds").isArray()
                        ? json.get("claimIds").size()
                        : 0;
                String done = rest.equals("/bulk-open") ? "opened" : rest.equals("/bulk-void") ? "voided" : "settled";
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"" + done + "\":0,\"refused\":" + selected + "}");
                return;
            }
            if (rest.equals("/bulk-grade") || rest.equals("/bulk-retire") || rest.equals("/bulk-hold")
                    || rest.equals("/generate-plans")) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(rest.equals("/bulk-grade") ? "{\"graded\":0}"
                        : rest.equals("/bulk-retire") ? "{\"retired\":0}"
                        : rest.equals("/bulk-hold") ? "{\"held\":0}" : "{\"made\":0}");
                return;
            }
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("text/plain");
            response.getWriter().write(viewingAsOf ? "The sheet is shown as of " + asOf.trim() + "; it cannot be changed"
                    : archived ? "The sheet is archived" : "A viewer cannot change the sheet");
        }

        private static JsonNode parse(byte[] body) {
            if (body.length == 0) {
                return null;
            }
            try {
                JsonNode node = JSON.readTree(body);
                return node != null && node.isObject() ? node : null;
            } catch (IOException e) {
                return null;
            }
        }
    }

    /** The request with its body, already read, readable again by the endpoint. */
    private static final class Replay extends HttpServletRequestWrapper {
        private final byte[] body;

        Replay(HttpServletRequest request, byte[] body) {
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
            String encoding = getCharacterEncoding() == null ? "UTF-8" : getCharacterEncoding();
            return new BufferedReader(new InputStreamReader(getInputStream(), java.nio.charset.Charset.forName(encoding)));
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
