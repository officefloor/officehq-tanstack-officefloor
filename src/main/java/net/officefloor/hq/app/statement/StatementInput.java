package net.officefloor.hq.app.statement;

import java.time.LocalDate;

/** A validated statement form; {@code asof} is null when left blank (the statement is taken now). */
public record StatementInput(LocalDate asof) {
}
