package net.officefloor.hq.app.claim;

import java.util.List;

/** Body of a bulk open: the selected claims' ids and who opens them. */
public record BulkOpenRequest(List<Long> claimIds, String user) {
}
