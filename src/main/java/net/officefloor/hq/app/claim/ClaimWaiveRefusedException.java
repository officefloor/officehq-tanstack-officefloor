package net.officefloor.hq.app.claim;

/** A waive by someone other than a steward, of a claim that is not OPEN or PENDING, or of one already waived. */
public class ClaimWaiveRefusedException extends Exception {

    public ClaimWaiveRefusedException(String message) {
        super(message);
    }
}
