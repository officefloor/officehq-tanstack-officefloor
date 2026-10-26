package net.officefloor.hq.app.claim;

/** A claim dated inside a closed period cannot be added, edited, opened, settled or voided. */
public class ClaimInClosedPeriodException extends Exception {

    public ClaimInClosedPeriodException(String message) {
        super(message);
    }
}
