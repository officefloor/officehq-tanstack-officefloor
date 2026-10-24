package net.officefloor.hq.app.sheet;

/** A today that is not a calendar date (YYYY-MM-DD). */
public class InvalidTodayException extends Exception {
    public InvalidTodayException(String value) {
        super("Not a date: " + value);
    }
}
