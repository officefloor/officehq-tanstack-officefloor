package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A retirement the person was unretired from: from its from-date to the day they were unretired. */
public record PastRetirement(LocalDate from, LocalDate to) {
    /** Whether a claim on the date is refused: it is after the from and before the to. */
    public boolean refuses(LocalDate date) { return date.isAfter(from) && date.isBefore(to); }
}
