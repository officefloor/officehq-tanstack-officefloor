package net.officefloor.hq.app.claim;

/** A claim cannot be added or edited to a date after its person's retirement date. */
public class ClaimAfterRetirementException extends Exception {

    public ClaimAfterRetirementException(String message) {
        super(message);
    }
}
