package net.officefloor.hq.app.sheet;

/** A today that is not a calendar date (YYYY-MM-DD), or a change of it by a role the permission matrix does not
 *  allow. */
public class InvalidTodayException extends Exception {
    public InvalidTodayException(String value) {
        super("Not a date: " + value);
    }

    private InvalidTodayException(String message, boolean refused) {
        super(message);
    }

    /** A change of the sheet's today refused for the role taking it. */
    public static InvalidTodayException refused(String role) {
        return new InvalidTodayException("A " + role + " cannot change the sheet's today.", true);
    }
}
