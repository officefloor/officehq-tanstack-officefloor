package net.officefloor.hq.app.statement;

import java.time.LocalDate;

/** The date a statement is taken as of; null {@code on}: the sheet's today, taken now. */
public record StatementAsOf(LocalDate on) {
}
