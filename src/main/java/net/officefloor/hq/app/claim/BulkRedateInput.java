package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.Set;

/** A validated bulk re-date: who is re-dating, the selected claim ids and their new date. */
public record BulkRedateInput(String user, Set<Long> claimIds, LocalDate date) {
}
