package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** One hold on a person: from its from-date (null: the beginning of time) to its to-date (null: open-ended),
 *  both included. */
public record Hold(LocalDate from, LocalDate to) {

    /** Whether the date falls within the hold. */
    public boolean covers(LocalDate date) {
        return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
    }
}
