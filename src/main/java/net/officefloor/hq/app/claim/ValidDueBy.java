package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** A due-by set directly, with its date parsed. */
public record ValidDueBy(String user, LocalDate dueBy) {
}
