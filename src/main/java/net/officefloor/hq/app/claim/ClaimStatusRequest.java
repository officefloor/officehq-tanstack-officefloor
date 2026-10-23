package net.officefloor.hq.app.claim;

/** Body of a status change: who is making it, the action (open, settle, approve, reject, review, void or resettle) and, for a
 *  reject, the reason typed, or for a void the reason chosen from the sheet's list, and for a settle the date it is settled
 *  as of (yyyy-mm-dd; blank: today). */
public record ClaimStatusRequest(String user, String action, String reason, String on) {
}
