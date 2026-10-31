package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.Set;

/** A validated bulk re-dating: the selected claims, their new date, and who re-dates them. */
public record BulkRedate(Set<Long> claimIds, LocalDate date, String user) {
}
