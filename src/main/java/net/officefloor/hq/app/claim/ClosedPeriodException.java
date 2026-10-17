package net.officefloor.hq.app.claim;

/** The claim is dated inside a closed period, so it cannot be added, edited, opened, settled or voided; nothing is changed. */
public class ClosedPeriodException extends Exception {
    public ClosedPeriodException(String message) {
        super(message);
    }
}
