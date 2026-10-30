package net.officefloor.hq.app.claim;

/** A claim form that cannot be saved (an amount that is not 0.00 or more with at most two decimals, a bad date); nothing is written. */
public class InvalidClaimException extends Exception {
    public InvalidClaimException(String message) {
        super(message);
    }
}
