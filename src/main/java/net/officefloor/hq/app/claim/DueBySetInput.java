package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** A validated due-by set: who is setting it and the claim's new due-by. */
public record DueBySetInput(String user, LocalDate dueBy) {
}
