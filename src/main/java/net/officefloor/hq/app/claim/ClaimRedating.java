package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** A re-dating of a claim: its new date and who re-dates it; once applied, also the date it had before. */
public record ClaimRedating(LocalDate date, String by, LocalDate old) {
}
