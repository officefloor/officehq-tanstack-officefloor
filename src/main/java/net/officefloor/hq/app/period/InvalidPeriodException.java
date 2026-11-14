package net.officefloor.hq.app.period;

/** The name is missing or already in use on the sheet, a date is missing or not a date, the from-date
 *  is after the to-date, or the dates overlap another period's: refused and nothing is written. */
public class InvalidPeriodException extends Exception {
    public InvalidPeriodException(String message) {
        super(message);
    }
}
