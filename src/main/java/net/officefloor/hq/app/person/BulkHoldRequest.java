package net.officefloor.hq.app.person;

import java.util.List;

/** Body of POST /api/sheets/{sheet}/people/bulk-hold: the ids of the selected people, the from-date (blank for the sheet's today) and the optional to-date. */
public record BulkHoldRequest(List<Long> people, String from, String to) {
}
