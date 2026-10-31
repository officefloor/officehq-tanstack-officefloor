package net.officefloor.hq.app.claim;

import java.util.List;

/** Body of POST /api/sheets/{sheetId}/claims/bulk-void: the selected claims' ids, the void reason chosen (when the
 *  sheet has void reasons) and the user voiding them. */
public record BulkVoidRequest(List<String> claimIds, String reason, String user) {
}
