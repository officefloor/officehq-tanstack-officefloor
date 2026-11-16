package net.officefloor.hq.app.claim;

/** A status change once validated: the action taken, the status it moves the claim to (a clerk's settle may yet
 *  make it PENDING), who makes it, and the reason a rejection gives (empty for any other action). */
public record ClaimStatusChange(ClaimAction action, ClaimStatus to, String by, String reason) {
}
