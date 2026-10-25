package net.officefloor.hq.app.claim;

import java.util.List;

/** The selected claims' ids to settle together, and the signed-in user (`by`) settling them. */
public record BulkSettleRequest(List<Long> ids, String by) {
}
