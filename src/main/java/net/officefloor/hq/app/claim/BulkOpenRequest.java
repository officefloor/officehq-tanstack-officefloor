package net.officefloor.hq.app.claim;

import java.util.List;

/** Body of a bulk open: the ids of the selected claims, in any order. */
public record BulkOpenRequest(List<Long> claimIds) {
}
