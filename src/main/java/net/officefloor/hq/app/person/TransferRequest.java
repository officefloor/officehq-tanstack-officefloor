package net.officefloor.hq.app.person;

/** Body of POST /api/sheets/{sheetId}/people/{personId}/transfer: the id of the sheet transferred to, and who transfers. */
public record TransferRequest(String toSheet, String user) {
}
