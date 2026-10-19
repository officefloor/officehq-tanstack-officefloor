package net.officefloor.hq.app.claim;

/** The claim form was refused: an amount that is missing or has more than two decimals, a date that is
 *  not a date, or a correction (a negative amount) that does not name a SETTLED claim of the same person. */
public class InvalidClaimException extends Exception {
    public InvalidClaimException(String message) {
        super(message);
    }
}
