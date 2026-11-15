package net.officefloor.hq.app.person;

import java.util.List;

/** Body of a bulk retire: the ids of the selected people, in any order, and the date they are
 *  retired from (blank: the sheet's today). */
public record BulkRetireRequest(List<Long> personIds, String from) {
}
