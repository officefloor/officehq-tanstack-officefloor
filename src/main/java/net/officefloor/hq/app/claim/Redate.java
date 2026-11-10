package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** A re-dating as validated: the claim's new date. */
public record Redate(LocalDate date) {
}
