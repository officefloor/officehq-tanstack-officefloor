package net.officefloor.hq.app.crew;

/** Body of POST /api/sheets/{sheet}/crews: the name of the crew to add and its lead's id. */
public record CrewRequest(String name, Long lead) {
}
