package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A validated hold's dates. {@code from} is null when it was left blank (the sheet's today); {@code to} is null for no end. */
public record HoldDates(LocalDate from, LocalDate to) {
}
