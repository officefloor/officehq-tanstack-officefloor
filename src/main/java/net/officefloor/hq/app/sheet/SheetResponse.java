package net.officefloor.hq.app.sheet;

import java.util.List;

/** A sheet as the screen shows it: its id, its today (ISO yyyy-MM-dd) whether it is archived, the number of its last bulk settle batch (null when none), and the tags claims carried on it before leaving it (sorted). */
public record SheetResponse(String id, String today, boolean archived, Integer lastBatch, List<String> tagsGone) {
}
