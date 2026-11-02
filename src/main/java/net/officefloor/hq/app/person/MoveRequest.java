package net.officefloor.hq.app.person;

/** Body of POST /api/sheets/{sheet}/people/{person}/move: the direction, up or down. */
public record MoveRequest(String direction) {
}
