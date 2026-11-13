package net.officefloor.hq.app.claim;

import java.time.LocalDate;

/** One line of a claim's status log: the status, the user who gave it, the sheet's today then and the claim's due then (two decimals, or n/a). */
public record ClaimLogLine(long claimId, ClaimStatus status, String by, LocalDate date, String due) {
}
