package net.officefloor.hq.app.statement;

/** Body of taking a person's statement: who is taking it, and the date it is taken as of (blank: now). */
public record StatementRequest(String user, String asOf) {
}
