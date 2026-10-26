package net.officefloor.hq.app.claim;

/** A re-settle by someone other than a steward, or of a claim that is not SETTLED. */
public class ClaimResettleRefusedException extends Exception {

    public ClaimResettleRefusedException(String message) {
        super(message);
    }
}
