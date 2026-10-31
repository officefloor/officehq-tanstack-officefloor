package net.officefloor.hq.app.claim;

import java.util.Set;

/** A validated bulk open: the selected claims and who opens them. */
public record BulkOpen(Set<Long> claimIds, String user) {
}
