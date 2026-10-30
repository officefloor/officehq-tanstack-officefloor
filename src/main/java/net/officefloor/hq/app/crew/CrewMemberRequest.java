package net.officefloor.hq.app.crew;

/** Body of POST /api/sheets/{sheetId}/crew-members: the crew and the id of the person to add to it. */
public record CrewMemberRequest(String crew, String personId, String user) {
}
