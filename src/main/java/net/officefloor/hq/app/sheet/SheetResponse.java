package net.officefloor.hq.app.sheet;

/** A sheet as the screen sees it; `today` is ISO yyyy-MM-dd; `archived` once a steward archived it. */
public record SheetResponse(String id, String today, boolean archived) {
}
