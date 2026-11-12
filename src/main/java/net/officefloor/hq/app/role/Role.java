package net.officefloor.hq.app.role;

import java.util.Locale;

/** A user's role on a sheet: every name is a clerk unless assigned otherwise. A clerk settles only
 *  claims within the sheet's clerk limit; a steward settles any. A viewer sees everything and may
 *  change nothing. A restricted user acts as a clerk, but its people list shows only the person with its
 *  name and the people of the crews that person leads or belongs to (every total is still over everyone). */
public enum Role {
    CLERK, STEWARD, VIEWER, RESTRICTED;

    /** The role as the screen and the audit file show it: clerk, steward, viewer or restricted. */
    public String shown() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The role named by the text (clerk, steward, viewer or restricted), or null when it names none. */
    public static Role of(String text) {
        for (Role r : values()) {
            if (r.shown().equals(text)) {
                return r;
            }
        }
        return null;
    }
}
