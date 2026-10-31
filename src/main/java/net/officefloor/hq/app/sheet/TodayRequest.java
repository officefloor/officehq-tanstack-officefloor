package net.officefloor.hq.app.sheet;

/** Body of a today change: the new date (ISO yyyy-MM-dd) and who makes it. */
public record TodayRequest(String today, String user) {
}
