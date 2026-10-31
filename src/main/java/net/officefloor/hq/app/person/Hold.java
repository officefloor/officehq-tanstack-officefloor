package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A hold on a person, from a date to a date (both inclusive; `to` null: open-ended). */
public record Hold(LocalDate from, LocalDate to) {

    /** Whether `date` falls within the hold. */
    public boolean covers(LocalDate date) {
        return !date.isBefore(from) && (to == null || !date.isAfter(to));
    }
}
