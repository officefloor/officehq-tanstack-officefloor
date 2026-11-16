package net.officefloor.hq.app.period;

/** A period that cannot be added: an empty name, a date that is not yyyy-MM-dd, a to-date before its from-date,
 *  a name already on the sheet, or dates overlapping another period of the sheet. */
public class InvalidPeriodException extends Exception {
    public InvalidPeriodException(String message) {
        super(message);
    }
}
