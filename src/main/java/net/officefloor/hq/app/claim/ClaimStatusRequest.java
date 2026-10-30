package net.officefloor.hq.app.claim;

/** Body of POST /api/sheets/{sheetId}/claims/{claimId}/status: action is open, settle or void. */
public record ClaimStatusRequest(String action, String user) {
}
