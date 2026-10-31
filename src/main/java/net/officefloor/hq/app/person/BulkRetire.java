package net.officefloor.hq.app.person;

import java.util.Set;

/** A validated bulk retire: the selected people, and the retirement as the row's retire would send it. */
public record BulkRetire(Set<Long> personIds, Retirement retirement) {
}
