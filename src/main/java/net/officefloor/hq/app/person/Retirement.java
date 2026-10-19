package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A valid retire request: the date to retire from, or null for the sheet's today. */
public record Retirement(LocalDate from) {
}
