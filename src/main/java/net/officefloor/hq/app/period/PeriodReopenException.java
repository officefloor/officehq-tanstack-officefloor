package net.officefloor.hq.app.period;

/** A period cannot be reopened: it is not closed, or the user's role may not reopen it. Refused and nothing
 *  is written. */
public class PeriodReopenException extends Exception {
    public PeriodReopenException(String message) {
        super(message);
    }
}
