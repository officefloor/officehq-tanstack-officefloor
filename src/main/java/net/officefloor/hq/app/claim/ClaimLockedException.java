package net.officefloor.hq.app.claim;

/** A change to a claim another user's open editor locks; answered 409. */
public class ClaimLockedException extends Exception {
    public ClaimLockedException(String holder) {
        super("This claim is locked: " + holder + " is editing it");
    }
}
