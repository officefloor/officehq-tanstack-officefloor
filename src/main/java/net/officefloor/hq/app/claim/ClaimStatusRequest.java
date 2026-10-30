package net.officefloor.hq.app.claim;

/** Body of POST /api/sheets/{sheetId}/claims/{claimId}/status: action is open, settle, approve, reject
 *  or void; reason is required to reject. */
public record ClaimStatusRequest(String action, String user, String reason) {
}
