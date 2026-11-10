package net.officefloor.hq.app.sheet;

/** A today that is not a yyyy-MM-dd date. */
public class InvalidTodayException extends Exception {
    public InvalidTodayException() {
        super("Today must be a date (yyyy-MM-dd)");
    }
}
