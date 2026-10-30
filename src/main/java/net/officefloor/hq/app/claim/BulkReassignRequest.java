package net.officefloor.hq.app.claim;

import java.util.List;

/** Body of POST /api/sheets/{sheetId}/claims/bulk-reassign: the selected claims' ids, the id of the person they move to, and who moves them. */
public record BulkReassignRequest(List<String> claimIds, String toPersonId, String user) {
}
