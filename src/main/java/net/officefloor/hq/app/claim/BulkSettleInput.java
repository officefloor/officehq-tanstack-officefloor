package net.officefloor.hq.app.claim;

import java.util.Set;

/** A validated bulk settle: who is settling and the selected claim ids. */
public record BulkSettleInput(String user, Set<Long> claimIds) {
}
