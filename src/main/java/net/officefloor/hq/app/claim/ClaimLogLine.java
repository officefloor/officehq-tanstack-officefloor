package net.officefloor.hq.app.claim;

/** One line of a claim's status log: the status it took, the user who made the change, the sheet's today then
 *  (yyyy-MM-dd), and the claim's due as computed at that moment (two decimals, or n/a). */
public record ClaimLogLine(ClaimStatus status, String by, String on, String due) {
}
