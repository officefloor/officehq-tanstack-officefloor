package net.officefloor.hq.app.statement;

/** A statement to take, as of a date (yyyy-MM-dd; blank: now), and the signed-in user (`by`) taking it. */
public record StatementRequest(String by, String asOf) {
}
