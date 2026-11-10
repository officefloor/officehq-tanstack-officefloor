package net.officefloor.hq.app.claim;

/** A claim with a missing, negative or over-precise amount, or a date that is not a date. */
public class InvalidClaimException extends Exception {
    public InvalidClaimException(String message) {
        super(message);
    }
}
