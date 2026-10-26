package net.officefloor.hq.app.claim;

import java.util.List;

/**
 * The selected claims' ids to void together, the void reason chosen from the sheet's list (blank: none), and the
 * signed-in user (`by`) voiding them.
 */
public record BulkVoidRequest(List<Long> ids, String reason, String by) {
}
