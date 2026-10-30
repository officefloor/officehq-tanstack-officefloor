package net.officefloor.hq.app.person;

/** Body of POST /api/sheets/{sheetId}/people/{personId}/hold: who holds (or releases) them. */
public record HoldRequest(String user) {
}
