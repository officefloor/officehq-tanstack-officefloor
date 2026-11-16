package net.officefloor.hq.app.role;

import java.time.LocalDate;

/** A delegation once validated: the user, the from- and to-date, and the steward who delegates. */
public record DelegationValues(String user, LocalDate from, LocalDate to, String by) {
}
