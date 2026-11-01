package net.officefloor.hq.app.holiday;

/** The holiday is not a valid date, or the date is already on the sheet's list. */
public class InvalidHolidayException extends Exception {
    public InvalidHolidayException(String message) {
        super(message);
    }
}
