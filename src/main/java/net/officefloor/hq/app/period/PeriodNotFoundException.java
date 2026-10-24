package net.officefloor.hq.app.period;

/** No period with that id on the sheet. */
public class PeriodNotFoundException extends Exception {
    public PeriodNotFoundException(String periodId) {
        super("No period " + periodId + ".");
    }
}
