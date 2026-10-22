package net.officefloor.hq.app.period;

/** A period add with no name or signed-in user, a date that is not yyyy-mm-dd, a from after its to, a name
 *  already in use on the sheet, or dates overlapping another period. */
public class InvalidPeriodException extends Exception {
    public InvalidPeriodException(String message) {
        super(message);
    }
}
