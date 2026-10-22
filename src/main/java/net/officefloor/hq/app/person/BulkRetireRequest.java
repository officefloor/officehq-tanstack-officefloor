package net.officefloor.hq.app.person;

import java.util.List;

/** Body of a bulk retire: who is retiring, the ids of the selected people and the date they are retired from
 *  (yyyy-mm-dd; null or blank: the sheet's today). */
public record BulkRetireRequest(String user, List<Long> personIds, String from) {
}
