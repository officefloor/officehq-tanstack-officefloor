package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** A claim's re-dating, for the audit: the date it had and the date it has now. */
public record Redated(LocalDate oldDate, LocalDate newDate) {
}
