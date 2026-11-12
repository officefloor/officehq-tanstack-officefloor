package net.officefloor.hq.app.role;

import java.util.Locale;

/** A user's role on a sheet: every name is a clerk unless assigned otherwise. A clerk settles only
 *  claims within the sheet's clerk limit; a steward settles any. A viewer sees everything and may
 *  change nothing. */
public enum Role {
    CLERK, STEWARD, VIEWER;

    /** The role as the screen and the audit file show it: clerk, steward or viewer. */
    public String shown() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The role named by the text (clerk, steward or viewer), or null when it names none. */
    public static Role of(String text) {
        for (Role r : values()) {
            if (r.shown().equals(text)) {
                return r;
            }
        }
        return null;
    }
}
