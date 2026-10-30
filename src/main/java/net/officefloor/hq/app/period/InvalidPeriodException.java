package net.officefloor.hq.app.period;

/** A period that cannot be added (no name, a name already on the sheet, a bad date, a from-date after
 *  its to-date, or dates overlapping another period); nothing is written. */
public class InvalidPeriodException extends Exception {
    public InvalidPeriodException(String message) {
        super(message);
    }
}
