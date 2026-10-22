package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.Set;

/** A validated bulk retire: who is retiring, the selected people and the retire date (null: the sheet's today). */
public record BulkRetireInput(String user, Set<Long> personIds, LocalDate from) {
}
