package net.officefloor.hq.app.claim;

import java.util.List;

/** The selected claims' ids to open together, and the signed-in user (`by`) opening them. */
public record BulkOpenRequest(List<Long> ids, String by) {
}
