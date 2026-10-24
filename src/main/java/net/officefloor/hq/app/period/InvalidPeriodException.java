package net.officefloor.hq.app.period;

/** A period that cannot be added: no name, a date that is not a calendar date, a to-date before its
 *  from-date, a name the sheet already has, or dates overlapping another period's. */
public class InvalidPeriodException extends Exception {
    public InvalidPeriodException(String message) {
        super(message);
    }
}
