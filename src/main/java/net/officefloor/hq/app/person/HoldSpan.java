package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** One of a person's holds: from its from-date to its to-date, both inclusive (null: open at that end). */
public record HoldSpan(LocalDate from, LocalDate to) {

    /** Whether the date falls within the hold. */
    public boolean covers(LocalDate on) {
        return (from == null || !on.isBefore(from)) && (to == null || !on.isAfter(to));
    }
}
