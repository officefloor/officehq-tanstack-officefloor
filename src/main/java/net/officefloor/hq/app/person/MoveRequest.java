package net.officefloor.hq.app.person;

/** Body of moving a person in the list: who is moving them, and which way ("up" or "down"). */
public record MoveRequest(String user, String direction) {
}
