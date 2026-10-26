package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A validated retirement date. {@code date} is null when it was left blank: the sheet's today. */
public record RetireFrom(LocalDate date) {
}
