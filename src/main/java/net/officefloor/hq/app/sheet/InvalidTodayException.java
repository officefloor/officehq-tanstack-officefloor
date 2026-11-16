package net.officefloor.hq.app.sheet;

/** A today that is not a real yyyy-MM-dd date. */
public class InvalidTodayException extends Exception {
    public InvalidTodayException(String message) {
        super(message);
    }
}
