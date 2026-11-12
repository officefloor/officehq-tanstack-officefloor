package net.officefloor.hq.app.claim;

import java.util.List;

/** Body of a bulk move, as sent: the ids of the selected claims, the id of the person to move them to, and who moves them. */
public record BulkReassignRequest(List<String> claimIds, String to, String user) {
}
