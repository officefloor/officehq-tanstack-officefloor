package net.officefloor.hq.app.claim;

/** A claim was re-dated by anyone but a steward, while not OPEN or PENDING, or to a date not real or in a closed period. */
public class ClaimRedateRefusedException extends Exception {

    public ClaimRedateRefusedException(String message) {
        super(message);
    }
}
