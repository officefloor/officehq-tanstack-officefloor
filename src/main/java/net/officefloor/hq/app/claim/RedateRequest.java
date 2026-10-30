package net.officefloor.hq.app.claim;

/** Body of POST /api/sheets/{sheetId}/claims/{claimId}/redate: who re-dates the claim, and its new date (yyyy-mm-dd). */
public record RedateRequest(String user, String date) {
}
