package net.officefloor.hq.app.statement;

/** A statement just cancelled: the due-total it had (n/a when none). */
public record CancelledStatement(String dueTotal) {
}
