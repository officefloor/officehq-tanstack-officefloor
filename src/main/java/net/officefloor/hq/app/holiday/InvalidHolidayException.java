package net.officefloor.hq.app.holiday;

/** A holiday that cannot be added (not a date, or a date already on the list); nothing is written. */
public class InvalidHolidayException extends Exception {
    public InvalidHolidayException(String message) {
        super(message);
    }
}
