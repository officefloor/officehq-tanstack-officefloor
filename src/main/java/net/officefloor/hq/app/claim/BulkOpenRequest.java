package net.officefloor.hq.app.claim;

import java.util.List;

/** The bulk open: the ids of the selected claims. */
public record BulkOpenRequest(List<Long> claimIds) {
}
