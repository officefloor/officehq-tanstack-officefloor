package net.officefloor.hq.app.claim;

/** A status change, or an edit, the claim's current status does not allow. */
public class InvalidClaimStatusException extends Exception {
    public InvalidClaimStatusException(ClaimStatus from, ClaimStatus to) {
        super("A " + from + " claim cannot become " + to);
    }

    /** An edit of a claim that is no longer a draft. */
    public InvalidClaimStatusException(ClaimStatus status) {
        super("A " + status + " claim cannot be edited");
    }

    /** Any other change its status does not allow. */
    public InvalidClaimStatusException(String message) {
        super(message);
    }
}
