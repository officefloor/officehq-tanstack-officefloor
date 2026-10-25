package net.officefloor.hq.app.sheet;

/** A sheet's today, as yyyy-MM-dd, and whether the sheet is archived. */
public record SheetTodayResponse(String id, String today, boolean archived) {
}
