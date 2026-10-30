package net.officefloor.hq.app.person;

/** Body of POST /api/sheets/{sheetId}/people and PUT /api/sheets/{sheetId}/people/{personId}. */
public record PersonRequest(String name, String grade, String user) {
}
