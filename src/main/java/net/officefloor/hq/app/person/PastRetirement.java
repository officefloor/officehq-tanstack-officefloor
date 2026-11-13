package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A retirement the person was unretired from: from its from-date to the sheet's today when they were unretired. */
public record PastRetirement(LocalDate from, LocalDate to) {

    /** Whether a claim on this date is refused by it: after its from-date and before its to-date. */
    public boolean refuses(LocalDate date) {
        return date.isAfter(from) && date.isBefore(to);
    }
}
