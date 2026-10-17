package net.officefloor.hq.app.sheet;

/** The requested today is not a real date; nothing is changed. */
public class InvalidTodayException extends Exception {
    public InvalidTodayException(String message) {
        super(message);
    }
}
