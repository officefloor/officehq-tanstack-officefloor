package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A person's own terms (days; null: none) from a date until their next one's; a null {@code from} applies from the
 *  beginning of time. */
public record PersonTerms(Integer days, LocalDate from) {
}
