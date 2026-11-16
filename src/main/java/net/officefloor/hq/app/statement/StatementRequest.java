package net.officefloor.hq.app.statement;

/** Body of taking a statement: who takes it, and the date it is taken as of (yyyy-MM-dd; blank: the sheet's today). */
public record StatementRequest(String by, String asOf) {
}
