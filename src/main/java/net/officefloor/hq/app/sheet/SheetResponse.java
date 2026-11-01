package net.officefloor.hq.app.sheet;

/** A sheet as the screen shows it: its id, its today (ISO yyyy-MM-dd) whether it is archived, and the number of its last bulk settle batch (null when none). */
public record SheetResponse(String id, String today, boolean archived, Integer lastBatch) {
}
