package net.officefloor.hq.app.holiday;

/** Body of a holiday added to the sheet: its date (yyyy-mm-dd) and who adds it. */
public record HolidayRequest(String date, String user) {
}
