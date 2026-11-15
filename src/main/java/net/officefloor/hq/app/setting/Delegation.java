package net.officefloor.hq.app.setting;

/** A steward's delegation of the steward role to a user, from a date to a date (yyyy-MM-dd, inclusive). */
public record Delegation(long id, String sheetId, String user, String from, String to, String by) {
}
