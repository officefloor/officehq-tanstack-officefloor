package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.Set;

/** The people selected to be retired together, and the date (null: the sheet's today). */
public record BulkRetire(Set<Long> ids, LocalDate date) {
}
