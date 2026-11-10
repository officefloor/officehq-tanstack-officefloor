package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A hold on a person: from a date until an optional to-date (null: open-ended), both inclusive. */
public record Hold(LocalDate from, LocalDate to) {

    /** Whether the date is within the hold. */
    public boolean covers(LocalDate date) {
        return !date.isBefore(from) && (to == null || !date.isAfter(to));
    }
}
