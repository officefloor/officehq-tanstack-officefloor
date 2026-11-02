package net.officefloor.hq.app.settings;

/** A steward's delegation of the steward role to a user, from a date to a date (yyyy-MM-dd, both inclusive). */
public record Delegation(String user, String by, String from, String to) {
}
