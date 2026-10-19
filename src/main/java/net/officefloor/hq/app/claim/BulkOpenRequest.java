package net.officefloor.hq.app.claim;

import java.util.List;

/** The claims selected to be opened together, by id. */
public record BulkOpenRequest(List<Long> claimIds) {
}
