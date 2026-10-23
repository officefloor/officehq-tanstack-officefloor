package net.officefloor.hq.app.claim;

import java.util.List;

/** Body of a bulk void: who is voiding, the ids of the selected claims and the void reason chosen from the sheet's
 *  list (blank or absent: none). */
public record BulkVoidRequest(String user, List<Long> claimIds, String reason) {
}
