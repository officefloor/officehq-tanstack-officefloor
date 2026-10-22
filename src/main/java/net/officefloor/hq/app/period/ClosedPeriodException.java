package net.officefloor.hq.app.period;

/** A claim add, edit or status change touching a date inside a closed period. */
public class ClosedPeriodException extends Exception {
    public ClosedPeriodException(Period period) {
        super("The period " + period.name() + " is closed: its claims cannot be changed.");
    }
}
