package net.officefloor.hq.app.claim;

import java.util.Set;

/** The claims selected to be settled together. */
public record BulkSettleIds(Set<Long> ids) {
}
