package net.officefloor.hq.app.sheet;

/** The date given for a sheet's today is not a valid yyyy-MM-dd date. */
public class InvalidTodayException extends Exception {
    public InvalidTodayException(String value) {
        super("Not a valid date: " + value);
    }
}
