package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** A claim's due-by set directly: the due-by it had before. */
public record ClaimDueBy(LocalDate old) {
}
