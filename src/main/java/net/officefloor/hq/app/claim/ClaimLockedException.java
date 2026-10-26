package net.officefloor.hq.app.claim;

/** Another user has the claim's editor open: it cannot be edited until they save or cancel it. */
public class ClaimLockedException extends Exception {

    public ClaimLockedException(String message) {
        super(message);
    }
}
