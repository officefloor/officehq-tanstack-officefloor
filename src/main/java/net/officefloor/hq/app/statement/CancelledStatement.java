package net.officefloor.hq.app.statement;

/** A statement just cancelled of a person: the due-total it had recorded (n/a with none). */
public record CancelledStatement(String name, String dueTotal) {
}
