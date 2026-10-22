package net.officefloor.hq.app.holiday;

/** A holiday add with no signed-in user, a date that is not a real yyyy-mm-dd date, or a date already on the list. */
public class InvalidHolidayException extends Exception {
    public InvalidHolidayException(String message) {
        super(message);
    }
}
