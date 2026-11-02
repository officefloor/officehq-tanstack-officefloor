package net.officefloor.hq.app.statement;

/** The statement form: the date the statement is taken as of (blank: now). */
public record StatementRequest(String asof) {
}
