package net.officefloor.hq.app.claim;

import java.util.List;

/** Body of POST /api/sheets/{sheetId}/claims/bulk-redate: the selected claims' ids, their new date (yyyy-mm-dd), and who re-dates them. */
public record BulkRedateRequest(List<String> claimIds, String date, String user) {
}
