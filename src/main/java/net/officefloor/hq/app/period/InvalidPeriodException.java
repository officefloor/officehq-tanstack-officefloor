package net.officefloor.hq.app.period;

/** The period form was refused: a blank name or one already used on the sheet, a from or to that is
 *  not a date, a from after its to, or dates overlapping another period of the sheet. */
public class InvalidPeriodException extends Exception {
    public InvalidPeriodException(String message) {
        super(message);
    }
}
