package net.officefloor.hq.app.sheet;

/** Body of POST /api/sheets/{sheet}/today: the new today (ISO yyyy-MM-dd). */
public record TodayRequest(String today) {
}
