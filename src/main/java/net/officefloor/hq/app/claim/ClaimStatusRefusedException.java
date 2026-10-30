package net.officefloor.hq.app.claim;

/** A status change the claim's current status does not allow; nothing is written. */
public class ClaimStatusRefusedException extends Exception {
    public ClaimStatusRefusedException(ClaimStatus from, ClaimStatus to) {
        super("A " + from + " claim cannot become " + to);
    }
}
