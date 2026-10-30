package net.officefloor.hq.app.claim;

/** A due-by was set by someone not a steward, on a claim not DRAFT, OPEN or PENDING, or to a date not allowed. */
public class ClaimDueBySetRefusedException extends Exception {

    public ClaimDueBySetRefusedException(String message) {
        super(message);
    }
}
