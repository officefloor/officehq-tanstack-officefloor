package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** A due-by as validated: the claim's new due-by. */
public record DueBy(LocalDate date) {
}
