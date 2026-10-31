package net.officefloor.hq.app.claim;

import java.util.List;

/** Body of a bulk re-dating: the selected claims' ids, their new date (yyyy-mm-dd), and who re-dates them. */
public record BulkRedateRequest(List<Long> claimIds, String date, String user) {
}
