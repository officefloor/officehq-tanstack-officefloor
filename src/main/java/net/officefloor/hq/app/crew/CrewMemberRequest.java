package net.officefloor.hq.app.crew;

/** Body of POST /api/sheets/{sheet}/crew-members: the crew and the id of the person to add to it. */
public record CrewMemberRequest(String crew, Long person) {
}
