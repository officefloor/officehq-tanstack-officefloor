package net.officefloor.hq.app.period;

import java.time.LocalDate;

/** A period that passed validation: a name and a from-date on or before its to-date, and its cap on each
 *  person's OPEN or PENDING claims in it (at least 1; null for none). */
public record ValidPeriod(String name, LocalDate from, LocalDate to, String user, Integer cap) {

    public ValidPeriod(String name, LocalDate from, LocalDate to, String user) {
        this(name, from, to, user, null);
    }
}
