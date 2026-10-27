package net.officefloor.hq.app.batch;

import java.util.List;

/** The body of recording a batch: the ids of the claims a bulk settle settled (to SETTLED or PENDING). */
public record SettleBatchRequest(List<Long> claimIds) {
}
