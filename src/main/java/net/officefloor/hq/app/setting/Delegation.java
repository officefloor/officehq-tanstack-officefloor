package net.officefloor.hq.app.setting;

import java.time.LocalDate;

/** The steward role delegated to a user name by a steward, from a from-date to a to-date (both inclusive). */
public record Delegation(String sheetId, String user, LocalDate from, LocalDate to, String by) {

    /** Whether the date is inside the delegation's range. */
    public boolean covers(LocalDate date) {
        return !date.isBefore(from) && !date.isAfter(to);
    }
}
