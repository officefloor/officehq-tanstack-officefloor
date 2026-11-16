package net.officefloor.hq.app.sheet;

/** Body of a change to a sheet's today: the date as yyyy-MM-dd, and who changes it. */
public record TodayRequest(String today, String by) {
}
