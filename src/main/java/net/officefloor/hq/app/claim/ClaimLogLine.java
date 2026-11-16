package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** One line of a claim's status log: the status it took, who made the change, the sheet's today when it was made, and
 *  the claim's due as shown at that moment (two decimals, or n/a). */
public record ClaimLogLine(long claimId, ClaimStatus status, String by, LocalDate date, String due) {
}
