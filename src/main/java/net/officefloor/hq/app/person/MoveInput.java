package net.officefloor.hq.app.person;

/** A validated move request: whether the person goes up the list (true) or down it. */
public record MoveInput(boolean up) {
}
