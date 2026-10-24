package net.officefloor.hq.app.claim;

/** A status change the claim's current status does not allow (or an unknown action). */
public class ClaimStatusException extends Exception {
    public ClaimStatusException(String message) {
        super(message);
    }
}
