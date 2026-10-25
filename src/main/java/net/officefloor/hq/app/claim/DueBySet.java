package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** A claim's due-by set directly, for the audit: the due-by it had and the due-by it has now. */
public record DueBySet(LocalDate oldDueBy, LocalDate newDueBy) {
}
