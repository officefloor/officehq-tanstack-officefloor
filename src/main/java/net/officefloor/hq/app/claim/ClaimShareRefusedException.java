package net.officefloor.hq.app.claim;

/** A share with the claim's own person, someone already holding one, or past 100.00 in all, or of a SETTLED, VOID or DELETED claim. */
public class ClaimShareRefusedException extends Exception {

    public ClaimShareRefusedException(String message) {
        super(message);
    }
}
