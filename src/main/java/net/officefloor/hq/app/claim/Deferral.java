package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** A deferral, read from its request: the date to defer the claim until. */
public record Deferral(LocalDate until) {
}
