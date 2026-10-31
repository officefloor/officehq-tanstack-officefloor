package net.officefloor.hq.app.settings;

import java.util.Locale;

/** A user name's role on a sheet: a clerk (the default for any name) or a steward. */
public enum Role {
    CLERK, STEWARD;

    public String show() {
        return name().toLowerCase(Locale.ROOT);
    }
}
