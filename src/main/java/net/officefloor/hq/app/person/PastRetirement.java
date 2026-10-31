package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A retirement that has ended: retired from `from`, unretired on `to` (the sheet's today then). */
public record PastRetirement(LocalDate from, LocalDate to) {

    /** Whether `date` is after the from and before the to. */
    public boolean covers(LocalDate date) {
        return date.isAfter(from) && date.isBefore(to);
    }
}
