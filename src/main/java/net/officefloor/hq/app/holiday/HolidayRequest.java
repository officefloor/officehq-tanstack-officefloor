package net.officefloor.hq.app.holiday;

/** A holiday's date and the signed-in user (`by`) adding it. */
public record HolidayRequest(String date, String by) {
}
