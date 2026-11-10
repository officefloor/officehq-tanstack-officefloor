package net.officefloor.hq.app.statement;

import java.time.LocalDate;

/** A validated statement: the date it is taken as of (null: now). */
public record StatementInput(LocalDate asOf) {
}
