package net.officefloor.hq.app.claim;

/** Body of POST /api/sheets/{sheetId}/people/{personId}/claims and PUT /api/sheets/{sheetId}/claims/{claimId}. */
public record ClaimRequest(String amount, String date, String note, String user, String corrects) {
}
