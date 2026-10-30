package net.officefloor.hq.app.sheet;

/** Body of POST /api/sheets/{sheetId}/today. A blank {@code today} resets it to the real date. */
public record TodayRequest(String today, String user) {
}
