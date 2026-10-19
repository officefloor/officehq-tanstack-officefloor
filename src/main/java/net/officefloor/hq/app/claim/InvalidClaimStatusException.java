package net.officefloor.hq.app.claim;

/** The claim's status does not allow the requested change (SETTLED and VOID are final). */
public class InvalidClaimStatusException extends Exception {
    public InvalidClaimStatusException(String message) {
        super(message);
    }
}
