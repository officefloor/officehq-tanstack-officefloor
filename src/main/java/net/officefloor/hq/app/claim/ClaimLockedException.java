package net.officefloor.hq.app.claim;

/** A claim whose editor another user holds open: no one else can open or save its editor until it is released. */
public class ClaimLockedException extends Exception {
    public ClaimLockedException(String message) {
        super(message);
    }
}
