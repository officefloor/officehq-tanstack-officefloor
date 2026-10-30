package net.officefloor.hq.app.sheet;

/** A today that is not an ISO date (YYYY-MM-DD); the change is refused and nothing is written. */
public class InvalidTodayException extends Exception {
    public InvalidTodayException(String value) {
        super("Not a date (YYYY-MM-DD): " + value);
    }
}
