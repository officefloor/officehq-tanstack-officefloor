package net.officefloor.hq.app.claim;

/** The claim's status does not allow the change (a SETTLED or VOID claim never changes), or the
 *  user may not make it (the one who opened a claim cannot settle it): refused and nothing is written. */
public class ClaimStatusException extends Exception {
    public ClaimStatusException(String message) {
        super(message);
    }
}
