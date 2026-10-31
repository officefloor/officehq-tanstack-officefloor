package net.officefloor.hq.app.claim;

/** Body of POST /api/sheets/{sheetId}/claims/{claimId}/due-by: who sets the claim's due-by, and the due-by (yyyy-mm-dd). */
public record DueByRequest(String user, String dueBy) {
}
