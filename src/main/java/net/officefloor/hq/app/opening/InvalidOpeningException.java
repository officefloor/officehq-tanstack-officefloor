package net.officefloor.hq.app.opening;

/** An opening-balance paste with no signed-in user, no lines, or a line that is not name,period,amount: a person on
 *  the sheet by name or alias, a period on the sheet, and an amount of two decimals, 0.00 or more. */
public class InvalidOpeningException extends Exception {
    public InvalidOpeningException(String message) {
        super(message);
    }
}
