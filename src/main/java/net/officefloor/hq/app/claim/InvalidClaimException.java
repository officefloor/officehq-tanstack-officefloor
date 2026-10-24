package net.officefloor.hq.app.claim;

/** A claim form that cannot be saved: a missing or negative amount, more than two decimals, or a bad date. */
public class InvalidClaimException extends Exception {
    public InvalidClaimException(String message) {
        super(message);
    }
}
