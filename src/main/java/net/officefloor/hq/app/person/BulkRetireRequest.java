package net.officefloor.hq.app.person;

import java.util.List;

/** The bulk retire: the ids of the selected people and the date they are retired from (yyyy-mm-dd; blank for the sheet's today). */
public record BulkRetireRequest(List<Long> personIds, String date) {
}
