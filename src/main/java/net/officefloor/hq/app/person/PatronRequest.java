package net.officefloor.hq.app.person;

/** Body of POST /api/sheets/{sheetId}/people/{personId}/patrons: the id of the person named as patron, and who names them. */
public record PatronRequest(String patronId, String user) {
}
