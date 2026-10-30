package net.officefloor.hq.app.claim;

import java.util.List;

/** Body of POST /api/sheets/{sheetId}/claims/bulk-settle: the selected claims' ids and the user settling them. */
public record BulkSettleRequest(List<String> claimIds, String user) {
}
