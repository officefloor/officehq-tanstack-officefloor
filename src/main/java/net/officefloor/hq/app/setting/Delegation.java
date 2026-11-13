package net.officefloor.hq.app.setting;

import java.time.LocalDate;

/** The steward role delegated by a steward ({@code by}) to a user name, from a from-date to a to-date, both inclusive. */
public record Delegation(String sheetId, String user, LocalDate from, LocalDate to, String by) {

    /** Whether the delegation is in force on the date. */
    public boolean covers(LocalDate date) {
        return !date.isBefore(from) && !date.isAfter(to);
    }
}
