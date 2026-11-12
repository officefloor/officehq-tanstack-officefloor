package net.officefloor.hq.app.holiday;

/** Body of POST /api/sheets/{sheetId}/holidays: the date (yyyy-mm-dd) and the user who adds it. */
public record HolidayRequest(String date, String user) {
}
