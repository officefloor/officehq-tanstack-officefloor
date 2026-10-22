package net.officefloor.hq.app.delegation;

import java.time.LocalDate;

/** A steward's delegation of the steward role to a user, from a from-date to a to-date (both included). */
public record Delegation(long id, String sheetId, String user, LocalDate from, LocalDate to, String by) {
}
