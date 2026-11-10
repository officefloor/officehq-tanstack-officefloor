package net.officefloor.hq.app.holiday;

/** A holiday, as the screen reads it. */
public record HolidayResponse(String date) {
    public static HolidayResponse from(Holiday h) {
        return new HolidayResponse(h.date().toString());
    }
}
