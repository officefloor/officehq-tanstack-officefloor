package net.officefloor.hq.app.sheet;

/** A sheet's today, as yyyy-MM-dd, whether the sheet is archived, and its parent sheet's id (null: none). */
public record SheetTodayResponse(String id, String today, boolean archived, String parent) {
}
