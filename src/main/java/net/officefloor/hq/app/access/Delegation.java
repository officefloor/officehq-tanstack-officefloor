package net.officefloor.hq.app.access;

import java.time.LocalDate;

/** A steward's (`by`) delegation of the steward role to a user, from a date to a date, both inclusive. */
public record Delegation(String sheetId, String user, LocalDate from, LocalDate to, String by) {
}
