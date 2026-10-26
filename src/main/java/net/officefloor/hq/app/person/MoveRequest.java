package net.officefloor.hq.app.person;

/** The way to move a person in the list (`up` or `down`), and the signed-in user (`by`) moving them. */
public record MoveRequest(String direction, String by) {
}
