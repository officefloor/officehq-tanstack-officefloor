package net.officefloor.hq.app.settings;

import java.util.Locale;

/** A user name's role on a sheet: a clerk (the default for any name), a steward, or a viewer (who sees
 *  everything and can change nothing). */
public enum Role {
    CLERK, STEWARD, VIEWER;

    public String show() {
        return name().toLowerCase(Locale.ROOT);
    }
}
