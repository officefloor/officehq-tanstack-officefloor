package net.officefloor.hq.app.holiday;

/** A holiday that may not be added: not a date, or already on the sheet's list. */
public class HolidayRefusedException extends Exception {

    public HolidayRefusedException(String message) {
        super(message);
    }
}
