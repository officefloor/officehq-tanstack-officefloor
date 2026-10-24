package net.officefloor.hq.app.role;

import java.time.LocalDate;

/** A validated delegate form: a non-blank user name and the dates (null: the sheet's today). */
public record DelegationInput(String user, LocalDate from, LocalDate to) {
}
