package net.officefloor.hq.app.statement;

/** The statement a steward cancelled: its due-total ({@code "n/a"} when it had none). */
public record CancelledStatement(String dueTotal) {
}
