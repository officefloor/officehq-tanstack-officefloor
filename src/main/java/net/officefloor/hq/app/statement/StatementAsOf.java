package net.officefloor.hq.app.statement;

import java.time.LocalDate;

/** The date a statement is taken as of (null: now, the sheet's today). */
public record StatementAsOf(LocalDate date) {
}
