package net.officefloor.hq.app.delegation;

import java.time.LocalDate;

/** A steward (by) lends the steward role to a user from a from-date to a to-date, both inclusive. */
public record Delegation(long id, String by, String user, LocalDate from, LocalDate to) {
}
