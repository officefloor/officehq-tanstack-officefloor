package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A steward's flag on a person: its reason, in force until its until-date (included). While the sheet's today is on or
 *  before it, only a steward may open a claim of the person; after it the flag shows nothing and has no effect. */
public record PersonFlag(String reason, LocalDate until) {

    public boolean inForceOn(LocalDate date) {
        return !date.isAfter(until);
    }
}
