package net.officefloor.hq.app.person;

/** A move of a person one place up or down the list: direction is up or down, by the signed-in user. */
public record MoveRequest(String direction, String by) {
}
