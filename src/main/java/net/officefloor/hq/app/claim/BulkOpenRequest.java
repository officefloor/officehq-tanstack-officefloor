package net.officefloor.hq.app.claim;

import java.util.List;

/** Body of a bulk open: who is opening and the ids of the selected claims. */
public record BulkOpenRequest(String user, List<Long> claimIds) {
}
