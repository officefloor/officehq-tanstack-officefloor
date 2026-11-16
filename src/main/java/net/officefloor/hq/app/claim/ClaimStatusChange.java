package net.officefloor.hq.app.claim;

/** A status change once validated: the action taken, the status it moves the claim to (a clerk's settle may yet
 *  make it PENDING), who makes it, the reason a rejection gives, or the void reason a void was given (empty for any other action, or a void
 *  given none). */
public record ClaimStatusChange(ClaimAction action, ClaimStatus to, String by, String reason) {
}
