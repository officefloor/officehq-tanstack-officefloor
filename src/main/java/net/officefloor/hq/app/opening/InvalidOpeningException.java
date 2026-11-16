package net.officefloor.hq.app.opening;

/** A paste of opening balances refused: a line that is not name,period,amount, a name that is no person's (nor alias)
 *  on the sheet, a period not on the sheet, an amount that is not 0.00 or more with two decimals, or no lines at all. */
public class InvalidOpeningException extends Exception {
    public InvalidOpeningException(String message) {
        super(message);
    }
}
