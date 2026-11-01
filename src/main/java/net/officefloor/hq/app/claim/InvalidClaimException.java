package net.officefloor.hq.app.claim;

/** The claim's amount is not a number of 0.00 or more with up to two decimals, or its date is not a date, or
 * the claim edited is not a DRAFT. */
public class InvalidClaimException extends Exception {
    public InvalidClaimException(String message) {
        super(message);
    }
}
