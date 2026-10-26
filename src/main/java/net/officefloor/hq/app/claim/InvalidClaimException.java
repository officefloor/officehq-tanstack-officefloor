package net.officefloor.hq.app.claim;

/** A claim was given without an amount of 0.00 or more in up to two decimals, or with a date that is not real. */
public class InvalidClaimException extends Exception {

    public InvalidClaimException(String message) {
        super(message);
    }
}
