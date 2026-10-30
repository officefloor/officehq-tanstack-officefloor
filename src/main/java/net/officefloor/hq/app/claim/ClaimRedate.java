package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** A claim's re-dating: the date it had before. */
public record ClaimRedate(LocalDate old) {
}
