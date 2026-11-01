package net.officefloor.hq.app.person;

import java.util.Set;

/** A validated bulk hold: the selected people, and the hold as the row's hold would send it. */
public record BulkHold(Set<Long> personIds, HoldChange change) {
}
