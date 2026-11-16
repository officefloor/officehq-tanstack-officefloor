package net.officefloor.hq.app.person;

import java.util.List;

/** Body of a bulk retire: the ids of the selected people, the date they are retired from (blank: the sheet's today),
 *  and who retires them. */
public record BulkRetireRequest(List<Long> personIds, String from, String by) {
}
