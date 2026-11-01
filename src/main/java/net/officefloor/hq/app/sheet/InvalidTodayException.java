package net.officefloor.hq.app.sheet;

/** The requested today is missing or not a valid yyyy-MM-dd date. */
public class InvalidTodayException extends Exception {
    public InvalidTodayException(String message) {
        super(message);
    }
}
