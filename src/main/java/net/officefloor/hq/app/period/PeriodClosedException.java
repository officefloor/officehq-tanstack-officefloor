package net.officefloor.hq.app.period;

/** The claim's date falls inside a closed period (or the period is already closed): refused and
 *  nothing is written. */
public class PeriodClosedException extends Exception {
    public PeriodClosedException(String message) {
        super(message);
    }
}
