package net.officefloor.hq.app.person;

import java.util.List;

/** Body of a bulk hold: the ids of the selected people, the from-date (blank: the sheet's today), the optional to-date,
 *  and who holds them. */
public record BulkHoldRequest(List<Long> personIds, String from, String to, String by) {
}
