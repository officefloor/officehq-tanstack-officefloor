package net.officefloor.hq.app.person;

/** A statement of a person still standing: its date (yyyy-MM-dd) and the due-total it recorded (or n/a). */
public record StatementLine(String date, String dueTotal) {
}
