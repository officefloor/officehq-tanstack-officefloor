package net.officefloor.hq.app.crew;

/** Body of POST /api/sheets/{sheetId}/crews: the name of the crew to add and the id of its lead. */
public record CrewRequest(String name, String leadId, String user) {
}
