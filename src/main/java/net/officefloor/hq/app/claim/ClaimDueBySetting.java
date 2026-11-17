package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** A due-by setting once its body is read: the new due-by, who sets it, and the claim's due-by before (null until known). */
public record ClaimDueBySetting(LocalDate dueBy, String by, LocalDate old) {
}
