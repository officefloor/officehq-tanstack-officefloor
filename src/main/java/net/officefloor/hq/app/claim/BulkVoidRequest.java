package net.officefloor.hq.app.claim;

import java.util.List;

/** Body of POST /api/sheets/{sheet}/claims/bulk-void: the ids of the selected claims and the void reason chosen (none
 * when the sheet has no void reasons). */
public record BulkVoidRequest(List<Long> claims, String reason) {
}
