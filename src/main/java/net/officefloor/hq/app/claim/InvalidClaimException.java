package net.officefloor.hq.app.claim;

/** The claim's amount is not a number with up to two decimals (a negative one only as a correction of a settled
 * claim of the same person), or its date is not a date, or
 * the claim edited is not a DRAFT. */
public class InvalidClaimException extends Exception {
    public InvalidClaimException(String message) {
        super(message);
    }
}
