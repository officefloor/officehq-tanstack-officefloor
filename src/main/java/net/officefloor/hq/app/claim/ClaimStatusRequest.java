package net.officefloor.hq.app.claim;

/** Body of a status change of a claim: the action (open, settle, approve, reject or void), who takes it, and the
 *  reason a rejection gives or the void reason chosen for a void, and the date (yyyy-MM-dd) a settle is made as of
 *  (blank: the sheet's today). */
public record ClaimStatusRequest(String action, String by, String reason, String on) {
}
