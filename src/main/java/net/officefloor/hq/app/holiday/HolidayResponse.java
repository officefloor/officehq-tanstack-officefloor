package net.officefloor.hq.app.holiday;

/** A holiday as the screen shows it: its date (YYYY-MM-DD). */
public record HolidayResponse(long id, String date) {

    public static HolidayResponse from(Holiday holiday) {
        return new HolidayResponse(holiday.getId(), holiday.getDate().toString());
    }
}
