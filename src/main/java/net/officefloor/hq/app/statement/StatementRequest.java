package net.officefloor.hq.app.statement;

/** The body of a statement: the date it is taken as of, yyyy-mm-dd; blank takes it now. */
public record StatementRequest(String asOf) {
}
