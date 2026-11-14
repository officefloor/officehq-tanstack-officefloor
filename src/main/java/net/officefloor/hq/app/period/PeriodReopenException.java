package net.officefloor.hq.app.period;

/** A period cannot be reopened: it is not closed, or the user is not a steward. Refused and nothing
 *  is written. */
public class PeriodReopenException extends Exception {
    public PeriodReopenException(String message) {
        super(message);
    }
}
