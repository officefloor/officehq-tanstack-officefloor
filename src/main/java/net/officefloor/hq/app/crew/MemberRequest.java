package net.officefloor.hq.app.crew;

/** Body of POST /api/sheets/{sheetId}/crews/{crewId}/members: the person and the user adding them. */
public record MemberRequest(Long personId, String user) {
}
