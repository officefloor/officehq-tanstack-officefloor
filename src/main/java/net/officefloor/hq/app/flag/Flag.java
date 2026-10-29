package net.officefloor.hq.app.flag;

import java.time.LocalDate;

/** A person's flag: its reason, in force while the sheet's today is on or before {@code until}. */
public record Flag(long personId, String reason, LocalDate until) {

    /** Whether the flag is in force on the date (on or before its until-date). */
    public boolean inForceOn(LocalDate date) {
        return date != null && !date.isAfter(until);
    }
}
