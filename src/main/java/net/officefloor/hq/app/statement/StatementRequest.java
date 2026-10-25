package net.officefloor.hq.app.statement;

/** A statement to take, and the signed-in user (`by`) taking it. */
public record StatementRequest(String by) {
}
