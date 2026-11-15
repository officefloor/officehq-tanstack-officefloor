package net.officefloor.hq.app.period;

/** A period cannot be locked or unlocked: the user is not a steward, or it is already so. Refused and
 *  nothing is written. */
public class PeriodLockException extends Exception {
    public PeriodLockException(String message) {
        super(message);
    }
}
