package net.officefloor.hq.app.claim;

/** The claim's amount is missing, negative or has more than two decimals, its date is not a date, or it is dated after its person's retirement date: refused and nothing is written. */
public class InvalidClaimException extends Exception {
    public InvalidClaimException(String message) {
        super(message);
    }
}
