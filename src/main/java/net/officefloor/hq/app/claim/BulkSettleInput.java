package net.officefloor.hq.app.claim;

import java.util.List;

/** A validated bulk settle: the selected claim ids, each once (an id that is not a number is
 *  null, and is refused), and who settles them. */
public record BulkSettleInput(List<Long> claimIds, String user) {
}
