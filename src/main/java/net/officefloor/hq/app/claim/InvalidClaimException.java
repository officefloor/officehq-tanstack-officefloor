package net.officefloor.hq.app.claim;

/** A claim's values that cannot be kept: an amount that is not 0.00 or more with up to two decimals, or a bad date. */
public class InvalidClaimException extends Exception {
    public InvalidClaimException(String message) {
        super(message);
    }
}
