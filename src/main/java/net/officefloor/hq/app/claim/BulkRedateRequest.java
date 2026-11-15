package net.officefloor.hq.app.claim;

import java.util.List;

/** Body of a bulk re-date: the ids of the selected claims, in any order, and the date (yyyy-MM-dd) they take. */
public record BulkRedateRequest(List<Long> claimIds, String date) {
}
