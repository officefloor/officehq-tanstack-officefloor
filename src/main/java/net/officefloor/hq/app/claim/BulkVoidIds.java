package net.officefloor.hq.app.claim;

import java.util.Set;

/** The claims selected to be voided together. */
public record BulkVoidIds(Set<Long> ids) {
}
