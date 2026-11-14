package net.officefloor.hq.app.person;

import java.time.LocalDate;

/**
 * A person's own terms in days, in force from {@code from} (null: from the beginning of time); {@code terms} null:
 * none of their own from then.
 */
public record TermsFrom(Integer terms, LocalDate from) {
}
