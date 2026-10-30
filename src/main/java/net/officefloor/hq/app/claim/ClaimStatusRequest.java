package net.officefloor.hq.app.claim;

/** Body of POST /api/sheets/{sheetId}/claims/{claimId}/status: action is open, settle, approve, reject
 *  or void; reason is required to reject, and to void
 *  (one on the sheet's list) while the sheet has void reasons. */
public record ClaimStatusRequest(String action, String user, String reason) {
}
