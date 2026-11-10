package net.officefloor.hq.app.holiday;

import java.util.List;

/** The holidays on a sheet, by date. */
public record HolidaysResponse(List<HolidayResponse> holidays) {
}
