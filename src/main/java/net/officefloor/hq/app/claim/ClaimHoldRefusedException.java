package net.officefloor.hq.app.claim;

/** A hold or release of a claim that is not OPEN or PENDING. */
public class ClaimHoldRefusedException extends Exception {

    public ClaimHoldRefusedException(String message) {
        super(message);
    }
}
