package net.officefloor.hq.app.claim;

/** A claim whose due is above the proof threshold in force on its date was settled or approved with no proof. */
public class ClaimProofNeededException extends Exception {

    public ClaimProofNeededException(String message) {
        super(message);
    }
}
