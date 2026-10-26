package net.officefloor.hq.app.claim;

/**
 * A claim was given without an amount in up to two decimals, with a date that is not real, as a negative amount naming no
 * claim it corrects (or a claim it cannot correct), or naming a claim to correct without a negative amount.
 */
public class InvalidClaimException extends Exception {

    public InvalidClaimException(String message) {
        super(message);
    }
}
