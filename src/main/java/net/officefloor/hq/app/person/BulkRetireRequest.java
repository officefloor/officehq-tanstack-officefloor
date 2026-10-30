package net.officefloor.hq.app.person;

import java.util.List;

/** Body of POST /api/sheets/{sheetId}/people/bulk-retire: the selected people's ids, the date they are
 *  retired from (YYYY-MM-DD, blank for the sheet's today) and the user retiring them. */
public record BulkRetireRequest(List<String> personIds, String date, String user) {
}
