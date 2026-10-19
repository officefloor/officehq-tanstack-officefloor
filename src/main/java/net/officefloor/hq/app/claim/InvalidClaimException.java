package net.officefloor.hq.app.claim;

/** The claim form was refused: an amount that is missing, negative or has more than two decimals,
 *  or a date that is not a date. */
public class InvalidClaimException extends Exception {
    public InvalidClaimException(String message) {
        super(message);
    }
}
