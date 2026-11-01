package net.officefloor.hq.app.person;

import java.util.List;

/** Body of a bulk hold: the selected people's ids, the date the hold is from (blank or null for the
 *  sheet's today), the date it is to (blank or null: open-ended) and who makes the change. */
public record BulkHoldRequest(List<Long> personIds, String from, String to, String user) {
}
