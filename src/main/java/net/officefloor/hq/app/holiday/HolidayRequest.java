package net.officefloor.hq.app.holiday;

/** Body of adding a holiday: its date (yyyy-MM-dd), and who adds it. */
public record HolidayRequest(String date, String by) {
}
