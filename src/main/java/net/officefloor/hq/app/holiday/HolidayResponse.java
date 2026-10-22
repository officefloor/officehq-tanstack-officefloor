package net.officefloor.hq.app.holiday;

/** A holiday as the screen shows it. */
public record HolidayResponse(long id, String date, String by) {
    public static HolidayResponse from(Holiday h) {
        return new HolidayResponse(h.id(), h.date().toString(), h.by());
    }
}
