package net.officefloor.hq.app.holiday;

/** A holiday with no date, a date not yyyy-MM-dd, or a date already on the sheet's list. */
public class InvalidHolidayException extends Exception {
    public InvalidHolidayException(String message) {
        super(message);
    }
}
