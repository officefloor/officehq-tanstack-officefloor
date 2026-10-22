package net.officefloor.hq.app.claim;

import java.util.Set;

/** A validated bulk open: who is opening and the selected claim ids. */
public record BulkOpenInput(String user, Set<Long> claimIds) {
}
