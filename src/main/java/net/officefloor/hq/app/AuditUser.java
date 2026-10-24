package net.officefloor.hq.app;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/** The signed-in name a change was made by, as the screen sends it in the URI-encoded X-User header. */
public final class AuditUser {

    private AuditUser() {
    }

    public static String decode(String header) {
        return header == null ? "" : URLDecoder.decode(header.replace("+", "%2B"), StandardCharsets.UTF_8).trim();
    }
}
