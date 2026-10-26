package net.officefloor.hq.app.claim;

/** A split of a claim that is not OPEN (or is deferred), of an amount not between 0.00 and the claim's, or by a role that may not open a claim. */
public class ClaimSplitRefusedException extends Exception {

    public ClaimSplitRefusedException(String message) {
        super(message);
    }
}
