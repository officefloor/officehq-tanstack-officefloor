package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** A claim's re-dating: the date it had and the date it has now. */
public record Redating(LocalDate from, LocalDate to) {
}
