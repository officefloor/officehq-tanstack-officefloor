package net.officefloor.hq.app.period;

/** Opening a claim whose person already has as many OPEN or PENDING claims in its period as the period's cap. */
public class CappedPeriodException extends Exception {
    public CappedPeriodException(Period period) {
        super("The period " + period.name() + " caps each person at " + period.cap()
                + " open claims: this person already has that many in it.");
    }
}
