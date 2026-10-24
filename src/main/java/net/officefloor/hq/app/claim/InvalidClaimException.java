package net.officefloor.hq.app.claim;

/** A claim form that cannot be saved: a missing amount, more than two decimals, a bad date, or a correction that
 * does not name a settled claim of its person. */
public class InvalidClaimException extends Exception {
    public InvalidClaimException(String message) {
        super(message);
    }
}
