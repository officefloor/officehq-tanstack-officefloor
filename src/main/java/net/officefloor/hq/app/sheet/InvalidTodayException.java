package net.officefloor.hq.app.sheet;

/** A today that is not an ISO date (YYYY-MM-DD); the change is refused and nothing is written. */
public class InvalidTodayException extends Exception {
    public InvalidTodayException(String value) {
        super("Not a date (YYYY-MM-DD): " + value);
    }

    private InvalidTodayException(String message, boolean plain) {
        super(message);
    }

    /** A change of today by a role the today permission does not allow. */
    public static InvalidTodayException notPermitted() {
        return new InvalidTodayException("Your role may not change the sheet's today", true);
    }
}
