package net.officefloor.hq.app.period;

/**
 * A period with no name, a name already on the sheet, a date that is not a date, a to-date before
 * its from-date, or dates that overlap another period on the sheet.
 */
public class InvalidPeriodException extends Exception {
    public InvalidPeriodException(String message) {
        super(message);
    }
}
