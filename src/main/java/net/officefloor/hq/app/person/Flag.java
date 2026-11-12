package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A flag on a person: its reason, in force until its until-date (inclusive). */
public record Flag(String reason, LocalDate until) {

    /** Whether the flag is in force on the date. */
    public boolean inForceOn(LocalDate date) {
        return date != null && !date.isAfter(until);
    }
}
