package net.officefloor.hq.app.claim;

/** A status change that names a known action: its target status, whether it is a steward's decision
 *  on a PENDING claim (approve or reject), the reason given to reject (or chosen from the sheet's list to void; null for none),
 *  and the date a settle is made as of (null: the sheet's today). */
public record ValidClaimStatus(ClaimStatus target, String user, Decision decision, String reason, java.time.LocalDate on) {

    public ValidClaimStatus(ClaimStatus target, String user, Decision decision, String reason) {
        this(target, user, decision, reason, null);
    }

    /** A steward's decision on a PENDING claim (or REVIEW on a REVIEW claim, opening it); NONE for any other status change. */
    public enum Decision { NONE, APPROVE, REJECT, REVIEW }
}
