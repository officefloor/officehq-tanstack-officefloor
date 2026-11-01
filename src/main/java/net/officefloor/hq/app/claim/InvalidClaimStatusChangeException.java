package net.officefloor.hq.app.claim;

/** The claim's status does not allow this change (a SETTLED or VOID claim cannot change status). */
public class InvalidClaimStatusChangeException extends Exception {
    public InvalidClaimStatusChangeException(String message) {
        super(message);
    }
}
