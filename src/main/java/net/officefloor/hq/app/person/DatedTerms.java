package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** Terms of a person's own, in days, recorded from a date on (null: none of their own from then). */
public record DatedTerms(LocalDate from, Integer terms) {
}
