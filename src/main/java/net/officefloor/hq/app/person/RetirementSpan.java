package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** One of a person's past retirements: from its from-date to the day they were unretired. */
public record RetirementSpan(LocalDate from, LocalDate to) {

    /** Whether the date falls strictly after the from-date and before the to-date. */
    public boolean refuses(LocalDate on) {
        return on.isAfter(from) && on.isBefore(to);
    }
}
