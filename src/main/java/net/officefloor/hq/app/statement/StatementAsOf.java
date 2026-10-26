package net.officefloor.hq.app.statement;

import java.time.LocalDate;

/** The date a statement is taken as of; {@code date} is null when it is taken now. */
public record StatementAsOf(LocalDate date) {
}
