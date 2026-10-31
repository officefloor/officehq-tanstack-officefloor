package net.officefloor.hq.app.claim;

/** Body of POST /api/sheets/{sheetId}/claims/{claimId}/review-assign: who assigns the REVIEW claim, and to whom. */
public record ReviewAssignRequest(String user, String reviewer) {
}
