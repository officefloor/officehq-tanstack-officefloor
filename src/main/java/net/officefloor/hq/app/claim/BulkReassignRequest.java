package net.officefloor.hq.app.claim;

import java.util.List;

/** A bulk move as the screen sends it: the ids of the selected claims and the id of the person they move to. */
public record BulkReassignRequest(List<String> claimIds, String personId) {
}
