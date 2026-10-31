package net.officefloor.hq.app.person;

/** Body of a statement: who takes it, and the date it is taken as of (yyyy-mm-dd; null or blank:
 *  now). Cancelling a statement takes no date. */
public record StatementRequest(String user, String asOf) {
}
