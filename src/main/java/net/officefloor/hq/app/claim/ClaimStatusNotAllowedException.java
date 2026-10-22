package net.officefloor.hq.app.claim;

/** A status change the claim's current status (or who opened it) does not allow. */
public class ClaimStatusNotAllowedException extends Exception {
    public ClaimStatusNotAllowedException(ClaimStatus from, ClaimStatus to) {
        super("A " + from + " claim cannot become " + to + ".");
    }

    public ClaimStatusNotAllowedException(String message) {
        super(message);
    }
}
