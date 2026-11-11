package net.officefloor.hq.app.person;

import java.time.LocalDate;

/** The last merge into a person: its id, the person merged, and the sheet's today it was made. */
public record LastMerge(long id, long fromId, LocalDate mergedOn) {
}
