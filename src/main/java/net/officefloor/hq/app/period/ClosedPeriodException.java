package net.officefloor.hq.app.period;

/** A claim dated inside a closed period: it cannot be added, edited, opened, settled or voided. */
public class ClosedPeriodException extends Exception {
    public ClosedPeriodException(String period) {
        super("A claim dated inside closed period " + period + " cannot be changed");
    }
}
