package net.officefloor.hq.app.session;

import net.officefloor.hq.app.BadRequestException;

/** The signed-in user a change is made by, as sent with the change; the audit records it. */
public record ChangedBy(String name) {

    /** The trimmed user, refusing a change sent without one. */
    public static ChangedBy of(String by) throws BadRequestException {
        String name = by == null ? "" : by.trim();
        if (name.isEmpty()) {
            throw new BadRequestException("Sign in to make a change");
        }
        return new ChangedBy(name);
    }
}
