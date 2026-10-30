package net.officefloor.hq.app.period;

import java.time.LocalDate;

/** A generate request that passed validation: a from-date on or before its to-date. */
public record ValidGenerate(LocalDate from, LocalDate to, String user) {
}
