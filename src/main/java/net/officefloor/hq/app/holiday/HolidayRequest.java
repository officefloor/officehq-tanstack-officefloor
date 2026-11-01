package net.officefloor.hq.app.holiday;

/** Body of POST /api/sheets/{sheet}/holidays: the date (yyyy-MM-dd) added to the sheet's holidays. */
public record HolidayRequest(String date) {
}
