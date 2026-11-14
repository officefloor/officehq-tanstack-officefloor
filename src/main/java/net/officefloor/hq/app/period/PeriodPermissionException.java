package net.officefloor.hq.app.period;

/** The user's role may not take the action on a period (the permission matrix). Refused and nothing
 *  is written. */
public class PeriodPermissionException extends Exception {
    public PeriodPermissionException(String message) {
        super(message);
    }
}
