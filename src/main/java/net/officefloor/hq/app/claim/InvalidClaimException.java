package net.officefloor.hq.app.claim;

/** The claim's amount is missing or has more than two decimals, it is a correction (negative) that does not name a SETTLED claim of its person by number, its date is not a date, or it is dated after its person's retirement date: refused and nothing is written. */
public class InvalidClaimException extends Exception {
    public InvalidClaimException(String message) {
        super(message);
    }
}
