package net.officefloor.hq.app.person;

/** A validated move: up the list (else down), and who moves them. */
public record MoveInput(boolean up, String user) {
}
