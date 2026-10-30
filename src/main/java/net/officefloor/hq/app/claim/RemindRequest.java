package net.officefloor.hq.app.claim;

/** Body of POST /api/sheets/{sheetId}/claims/{claimId}/remind: who reminds the claim. */
public record RemindRequest(String user) {
}
