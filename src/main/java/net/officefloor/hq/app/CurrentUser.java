package net.officefloor.hq.app;

import net.officefloor.server.http.HttpHeader;
import net.officefloor.server.http.ServerHttpConnection;

/** The signed-in user of a request: the name the front end sends in the X-User header. */
public final class CurrentUser {
    public static final String HEADER = "X-User";

    private CurrentUser() {
    }

    /** The trimmed name, refused when blank. A step takes the {@link ServerHttpConnection} and
     *  passes it here (header names are matched ignoring case, as browsers send them lower-case). */
    public static String require(ServerHttpConnection connection) throws BadRequestException {
        String name = "";
        for (HttpHeader header : connection.getRequest().getHeaders()) {
            if (HEADER.equalsIgnoreCase(header.getName())) {
                name = header.getValue() == null ? "" : header.getValue().trim();
                break;
            }
        }
        if (name.isEmpty()) {
            throw new BadRequestException("not signed in");
        }
        return name;
    }
}
