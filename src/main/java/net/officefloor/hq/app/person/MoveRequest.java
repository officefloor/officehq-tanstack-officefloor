package net.officefloor.hq.app.person;

/** Body of a move, as sent: the direction ("up" or "down") and who makes the change. */
public record MoveRequest(String direction, String user) {
}
