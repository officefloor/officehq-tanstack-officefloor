package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A merge kept for its undo: the person merged (their id and name), who merged them, on what sheet-today, and whether it was undone. */
public record MergeKept(long id, long fromId, String name, String mergedBy, LocalDate mergedOn, boolean undone) {
}
