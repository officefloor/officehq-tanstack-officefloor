package net.officefloor.hq.app.person;

/** Body of a move: "up" swaps the person with the one above them in the list, "down" with the one below. */
public record MoveRequest(String direction) {
}
