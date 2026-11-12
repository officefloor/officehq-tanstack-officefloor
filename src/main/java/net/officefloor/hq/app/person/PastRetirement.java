package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A past retirement of a person: from the date they were retired from to the day they were unretired. */
public record PastRetirement(LocalDate from, LocalDate to) {

    /** Whether the date falls strictly between the from-date and the to-date. */
    public boolean bars(LocalDate date) {
        return date.isAfter(from) && date.isBefore(to);
    }
}
