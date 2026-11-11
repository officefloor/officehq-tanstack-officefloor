package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** A settle: the date the claim is settled as of; null for the sheet's today. */
public record Settlement(LocalDate on) {
}
