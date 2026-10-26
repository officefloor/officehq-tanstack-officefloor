package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.Set;

/** The claims selected to be re-dated together, and the date they are re-dated to. */
public record BulkRedateIds(Set<Long> ids, LocalDate date) {
}
