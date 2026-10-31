package net.officefloor.hq.app.holiday;

import java.time.LocalDate;

/** A holiday as the screen sees it: its date as yyyy-mm-dd. */
public record HolidayResponse(String date) {
    public static HolidayResponse from(LocalDate date) {
        return new HolidayResponse(date.toString());
    }
}
