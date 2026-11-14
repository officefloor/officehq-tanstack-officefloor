package net.officefloor.hq.app.sheet;

/** The requested today is not a date: the change is refused and nothing is written. */
public class InvalidSheetTodayException extends Exception {
    public InvalidSheetTodayException(String message) {
        super(message);
    }
}
