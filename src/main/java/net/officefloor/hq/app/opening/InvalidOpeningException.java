package net.officefloor.hq.app.opening;

/** A paste of opening balances that cannot go through: no line, a line that is not name,period,amount, a person or
 *  period not on the sheet, or an amount that is not 0.00 or more with up to two decimals. */
public class InvalidOpeningException extends Exception {
    public InvalidOpeningException(String message) {
        super(message);
    }
}
