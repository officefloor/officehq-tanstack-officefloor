package net.officefloor.hq.app.claim;

import java.util.List;

/** How many of the selected claims were settled (or went to PENDING) and how many refused, with the sheet's claims after and the
 * number of its last batch (null when none). */
public record BulkSettleResponse(int settled, int refused, List<Claim> claims, Integer lastBatch) {
}
