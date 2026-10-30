package net.officefloor.hq.app.person;

/** Body of POST /api/sheets/{sheetId}/people/{personId}/unretire: who unretires them. */
public record UnretireRequest(String user) {
}
