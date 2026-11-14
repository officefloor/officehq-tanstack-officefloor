package net.officefloor.hq.app.snapshot;

import java.time.LocalDate;

/** That the person {@code personId}'s due-total (as shown) stood at {@code dueTotal} as the sheet left {@code day}. */
public record Snapshot(String sheetId, long personId, LocalDate day, String dueTotal) {
}
