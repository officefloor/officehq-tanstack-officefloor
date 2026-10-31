package net.officefloor.hq.app.person;

import java.util.List;

/** Body of a bulk retire: the selected people's ids, the date retired from (yyyy-mm-dd; blank or
 *  null for the sheet's today) and who makes the change. */
public record BulkRetireRequest(List<Long> personIds, String from, String user) {
}
