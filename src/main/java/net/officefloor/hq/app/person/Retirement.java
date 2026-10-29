package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A past retirement of a person: from the date they were retired from to the day they were unretired. */
public record Retirement(LocalDate from, LocalDate to) {

    /** Whether the date falls strictly between the from-date and the to-date (a claim then is refused). */
    public boolean excludes(LocalDate date) {
        return date.isAfter(from) && date.isBefore(to);
    }
}
