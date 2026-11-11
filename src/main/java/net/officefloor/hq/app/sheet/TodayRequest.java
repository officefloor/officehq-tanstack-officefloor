package net.officefloor.hq.app.sheet;

/** Body of POST /api/sheets/{sheetId}/today: the new today (yyyy-MM-dd) and who sets it. */
public record TodayRequest(String today, String user) {
}
