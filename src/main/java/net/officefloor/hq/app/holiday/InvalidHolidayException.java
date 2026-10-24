package net.officefloor.hq.app.holiday;

/** A holiday that cannot be added: a date that is not a calendar date, or one already on the sheet's list. */
public class InvalidHolidayException extends Exception {
    public InvalidHolidayException(String message) {
        super(message);
    }
}
