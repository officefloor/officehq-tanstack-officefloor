package net.officefloor.hq.app.claim;

import java.util.Set;

/** The claims selected to be opened together. */
public record BulkOpenIds(Set<Long> ids) {
}
