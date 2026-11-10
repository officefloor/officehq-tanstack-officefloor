package net.officefloor.hq.app.sheet;

/** A change of the sheet's today, as the screen sends it: {@code today} is yyyy-MM-dd. */
public record TodayRequest(String today) {
}
