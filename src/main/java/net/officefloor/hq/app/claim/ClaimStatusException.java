package net.officefloor.hq.app.claim;

/** The claim's status does not allow the change (a SETTLED or VOID claim never changes): refused and nothing is written. */
public class ClaimStatusException extends Exception {
    public ClaimStatusException(String message) {
        super(message);
    }
}
