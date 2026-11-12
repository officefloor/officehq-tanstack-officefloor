package net.officefloor.hq.app.claim;

import java.util.List;

/** Body of a bulk void, as sent: the ids of the selected claims, who voids them and the void reason
 *  (blank for none). */
public record BulkVoidRequest(List<String> claimIds, String user, String reason) {
}
