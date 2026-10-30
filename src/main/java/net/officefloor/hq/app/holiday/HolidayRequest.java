package net.officefloor.hq.app.holiday;

/** Body of POST /api/sheets/{sheetId}/holidays: the date (YYYY-MM-DD) to add to the sheet's holidays. */
public record HolidayRequest(String date, String user) {
}
