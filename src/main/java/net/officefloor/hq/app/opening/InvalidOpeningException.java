package net.officefloor.hq.app.opening;

/** An opening import that cannot be taken (a malformed line, a person or period not on the sheet, an amount
 *  that is not 0.00 or more with at most two decimals); nothing is written. */
public class InvalidOpeningException extends Exception {
    public InvalidOpeningException(String message) {
        super(message);
    }
}
