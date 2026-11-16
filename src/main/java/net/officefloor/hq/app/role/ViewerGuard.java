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
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A viewer changes nothing: every change to a sheet (any non-GET request under {@code /api/sheets/<id>/}) made
 * by a user whose role on that sheet is viewer is refused (403) before it reaches its endpoint, so nothing is
 * stored and nothing audited. A bulk action instead goes through with nothing done: 0 done, every selected claim
 * refused.
 */
@Configuration
public class ViewerGuard {

    private static final Pattern SHEET_PATH = Pattern.compile("^/api/sheets/([^/]+)(/.*)?$");
    private static final ObjectMapper JSON = new ObjectMapper();

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> viewerGuardFilter(RoleRepository roles) {
        FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>(new Filter(roles));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    private static final class Filter extends OncePerRequestFilter {
        private final RoleRepository roles;

        Filter(RoleRepository roles) {
            this.roles = roles;
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
            if (!RoleRepository.VIEWER.equals(roles.roleOf(sheetId, by))) {
                chain.doFilter(new Replay(request, body), response);
                return;
            }
            if (rest.equals("/bulk-open") || rest.equals("/bulk-settle")) {
                int selected = json != null && json.get("claimIds") != null && json.get("claimIds").isArray()
                        ? json.get("claimIds").size()
                        : 0;
                String done = rest.equals("/bulk-open") ? "opened" : "settled";
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"" + done + "\":0,\"refused\":" + selected + "}");
                return;
            }
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("text/plain");
            response.getWriter().write("A viewer cannot change the sheet");
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
