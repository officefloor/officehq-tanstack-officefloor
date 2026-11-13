package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** A validated due-by setting: the claim's new due-by and who sets it. */
public record DueByInput(LocalDate dueBy, String user) {
}
