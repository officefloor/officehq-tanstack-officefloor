package net.officefloor.hq.app.claim;

import java.util.List;

/** Body of POST /api/sheets/{sheetId}/claims/bulk-open: the selected claims' ids and the user opening them. */
public record BulkOpenRequest(List<String> claimIds, String user) {
}
