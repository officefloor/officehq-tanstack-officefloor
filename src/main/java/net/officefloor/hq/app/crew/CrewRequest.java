package net.officefloor.hq.app.crew;

/** Body of POST /api/sheets/{sheetId}/crews: the crew's name, its lead (a person's id) and the user adding it. */
public record CrewRequest(String name, Long leadId, String user) {
}
