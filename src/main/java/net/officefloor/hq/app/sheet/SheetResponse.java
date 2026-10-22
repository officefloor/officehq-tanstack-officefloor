package net.officefloor.hq.app.sheet;

/** A sheet: its id, its today (ISO yyyy-MM-dd) and whether it is archived. */
public record SheetResponse(String id, String today, boolean archived) {
}
