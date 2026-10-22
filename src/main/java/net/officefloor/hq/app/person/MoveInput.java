package net.officefloor.hq.app.person;

/** A validated move: who is moving the person, and whether up (else down). */
public record MoveInput(String user, boolean up) {
}
