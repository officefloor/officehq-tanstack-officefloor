package net.officefloor.hq.app.claim;

/** The claim's editor is open for another user, who holds its lock: refused and nothing is written. */
public class ClaimLockedException extends Exception {
    public ClaimLockedException(String message) {
        super(message);
    }
}
