package net.officefloor.hq.app.person;

/** Body of a move in the list, as typed: the direction ("up" or "down") and who moves them. */
public record MoveRequest(String direction, String user) {
}
