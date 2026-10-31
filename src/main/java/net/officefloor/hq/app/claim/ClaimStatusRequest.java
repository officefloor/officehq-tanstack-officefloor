package net.officefloor.hq.app.claim;

/** Body of POST /api/sheets/{sheetId}/claims/{claimId}/status: action is open, settle, approve, reject,
 *  void, delete or restore; reason is required to reject, and to void
 *  (one on the sheet's list) while the sheet has void reasons; on is the date to settle as of ("" or absent: the sheet's today). */
public record ClaimStatusRequest(String action, String user, String reason, String on) {
}
