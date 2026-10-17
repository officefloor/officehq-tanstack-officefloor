package net.officefloor.hq.app.period;

/** The requested period is not valid (no name, a name taken, bad dates, or overlapping another); nothing is changed. */
public class InvalidPeriodException extends Exception {
    public InvalidPeriodException(String message) {
        super(message);
    }
}
