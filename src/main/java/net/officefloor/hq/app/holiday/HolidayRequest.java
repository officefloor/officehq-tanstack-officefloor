package net.officefloor.hq.app.holiday;

/** Body of a holiday add: who is adding it and the date as typed. */
public record HolidayRequest(String user, String date) {
}
