package net.officefloor.hq.app.settings;

import java.util.Locale;

/** A user name's role on a sheet: a clerk (the default for any name), a steward, a viewer (who sees
 *  everything and can change nothing), or a restricted clerk (whose people list holds only the people
 *  of the crews they lead or belong to, and themselves). */
public enum Role {
    CLERK, STEWARD, VIEWER, RESTRICTED;

    public String show() {
        return name().toLowerCase(Locale.ROOT);
    }
}
