package net.officefloor.hq.app.period;

/** The period has no name or a name already on the sheet, a bad date, a to-date before its from-date, or overlaps another period. */
public class InvalidPeriodException extends Exception {
    public InvalidPeriodException(String message) {
        super(message);
    }
}
