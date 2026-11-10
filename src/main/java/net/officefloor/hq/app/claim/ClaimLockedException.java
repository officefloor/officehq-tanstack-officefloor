package net.officefloor.hq.app.claim;

/** A claim whose editor another user has open: it cannot be edited until they save or cancel. */
public class ClaimLockedException extends Exception {
    public ClaimLockedException(String claimId) {
        super("Claim " + claimId + " is being edited by another user");
    }
}
