package net.officefloor.hq.app.claim;

/** A status change the claim's status does not allow, or an action that is not open, settle or void. */
public class InvalidClaimStatusException extends Exception {
    public InvalidClaimStatusException(String message) {
        super(message);
    }
}
