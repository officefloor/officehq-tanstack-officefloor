package net.officefloor.hq.app.person;

import java.util.List;

/** Body of POST /api/sheets/{sheet}/people/bulk-retire: the ids of the selected people and the date retired from (blank for the sheet's today). */
public record BulkRetireRequest(List<Long> people, String from) {
}
