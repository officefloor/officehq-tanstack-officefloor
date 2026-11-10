package net.officefloor.hq.app.person;

import java.util.List;

/** The bulk hold: the ids of the selected people, the from-date (yyyy-mm-dd; blank for the sheet's today)
 *  and the to-date (yyyy-mm-dd; blank for open-ended). */
public record BulkHoldRequest(List<Long> personIds, String from, String to) {
}
