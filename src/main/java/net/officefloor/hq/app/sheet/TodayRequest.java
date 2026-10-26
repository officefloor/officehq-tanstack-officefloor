package net.officefloor.hq.app.sheet;

/** The body of a change to a sheet's today: an ISO yyyy-MM-dd date, blank for the real date. */
public record TodayRequest(String today) {
}
