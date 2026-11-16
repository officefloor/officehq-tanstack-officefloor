package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** A merge kept on the sheet (see PersonRepository#merge): who was merged into whom, by whom and on
 *  which of the sheet's todays, whether it can be undone and whether it has been. */
public record PersonMerge(long id, String sheetId, long fromId, String fromName, long intoId, String mergedBy,
        LocalDate mergedOn, boolean undoable, boolean undone, Long nameAliasId) {
}
