package net.officefloor.hq.app.sheet;

/** Body of a today change: who is changing it, and the new date (ISO yyyy-MM-dd). */
public record TodayRequest(String user, String today) {
}
