package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.Set;

/** A validated bulk hold: who is holding, the selected people, the from-date (null: the sheet's today) and the
 *  to-date (null: open-ended). */
public record BulkHoldInput(String user, Set<Long> personIds, LocalDate from, LocalDate to) {
}
