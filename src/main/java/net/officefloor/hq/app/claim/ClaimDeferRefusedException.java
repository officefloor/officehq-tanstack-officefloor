package net.officefloor.hq.app.claim;

/** A claim was deferred while not OPEN, or to a date that is not real or not after the sheet's today. */
public class ClaimDeferRefusedException extends Exception {

    public ClaimDeferRefusedException(String message) {
        super(message);
    }
}
