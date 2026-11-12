package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A hold on a person: from its from-date to its to-date (null: no end), both inclusive. */
public record Hold(LocalDate from, LocalDate to) {

    /** Whether the date falls within the hold. */
    public boolean covers(LocalDate date) {
        return !date.isBefore(from) && (to == null || !date.isAfter(to));
    }
}
