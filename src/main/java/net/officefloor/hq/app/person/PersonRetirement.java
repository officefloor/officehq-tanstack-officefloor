package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A past retirement of a person: from their retirement date to the sheet's today when they were unretired. */
public record PersonRetirement(LocalDate from, LocalDate to) {

    /** Whether the date falls after the from and before the to: no claim may be dated there. */
    public boolean excludes(LocalDate date) {
        return date.isAfter(from) && date.isBefore(to);
    }
}
