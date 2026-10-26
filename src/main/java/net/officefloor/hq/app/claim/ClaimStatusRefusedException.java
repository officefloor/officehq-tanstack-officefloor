package net.officefloor.hq.app.claim;

/** A claim was asked to change to a status its current status does not allow. */
public class ClaimStatusRefusedException extends Exception {

    public ClaimStatusRefusedException(String message) {
        super(message);
    }
}
