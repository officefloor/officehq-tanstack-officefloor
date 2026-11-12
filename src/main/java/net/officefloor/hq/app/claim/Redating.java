package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** A stored re-dating: the date the claim had and the date it has now. */
public record Redating(LocalDate old, LocalDate now) {
}
