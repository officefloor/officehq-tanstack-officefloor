package net.officefloor.hq.app.claim;

/** A status change the claim's current status does not allow; answered 409. */
public class ClaimStatusRefusedException extends Exception {
    public ClaimStatusRefusedException(ClaimStatus from, ClaimAction action) {
        super("A " + from + " claim cannot be " + action.to());
    }

    public ClaimStatusRefusedException(String message) {
        super(message);
    }
}
