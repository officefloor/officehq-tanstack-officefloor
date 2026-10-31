package net.officefloor.hq.app.person;

import java.util.List;

/** Body of POST /api/sheets/{sheetId}/people/bulk-hold: the selected people's ids, the hold's from-date
 *  (blank: the sheet's today), its optional to-date (blank: open-ended) and who holds them. */
public record BulkHoldRequest(List<String> personIds, String from, String to, String user) {
}
