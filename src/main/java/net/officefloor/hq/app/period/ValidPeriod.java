package net.officefloor.hq.app.period;

import java.time.LocalDate;

/** A period that passed validation: a name and a from-date on or before its to-date. */
public record ValidPeriod(String name, LocalDate from, LocalDate to, String user) {
}
