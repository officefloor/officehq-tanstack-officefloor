package net.officefloor.hq.app.claim;

/** A validated status change: who is making it, the action (open, settle, approve, reject, void or resettle), the status
 *  the claim is to have (a settle above the clerk limit makes it PENDING instead) and, for a reject, the reason, and for a void the
 *  reason chosen from the sheet's list of void reasons (null otherwise), and for a settle the date it is settled as of (null:
 *  as it is now). */
public record ClaimStatusInput(String user, String action, ClaimStatus to, String reason, java.time.LocalDate on) {
    public ClaimStatusInput(String user, String action, ClaimStatus to, String reason) {
        this(user, action, to, reason, null);
    }
}
