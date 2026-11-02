package net.officefloor.hq.app.period;

import java.time.LocalDate;

/** A validated period body: a name and a from and to date, inclusive, the from on or before the to, and a cap
 *  of open claims per person (a whole number of at least 1, or null for none). */
public record PeriodInput(String name, LocalDate from, LocalDate to, Integer cap) {

    public PeriodInput(String name, LocalDate from, LocalDate to) {
        this(name, from, to, null);
    }
}
