package net.officefloor.hq.app.claim;

import java.util.List;

/** Body of a bulk re-date: who is re-dating, the ids of the selected claims and their new date (yyyy-mm-dd). */
public record BulkRedateRequest(String user, List<Long> claimIds, String date) {
}
