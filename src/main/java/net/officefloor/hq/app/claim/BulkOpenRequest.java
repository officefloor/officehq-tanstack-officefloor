package net.officefloor.hq.app.claim;

import java.util.List;

/** Body of a bulk open, as sent: the ids of the selected claims and who opens them. */
public record BulkOpenRequest(List<String> claimIds, String user) {
}
