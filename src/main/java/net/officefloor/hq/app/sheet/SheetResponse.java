package net.officefloor.hq.app.sheet;

/** A sheet as the screen shows it: its id and its today (ISO yyyy-MM-dd). */
public record SheetResponse(String id, String today) {
}
