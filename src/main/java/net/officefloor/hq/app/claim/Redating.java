package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** A claim's re-date: the date it had and the date it now has. */
public record Redating(LocalDate from, LocalDate to) {
}
