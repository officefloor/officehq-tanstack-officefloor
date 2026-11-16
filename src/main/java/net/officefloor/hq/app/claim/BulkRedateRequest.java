package net.officefloor.hq.app.claim;

import java.util.List;

/** Body of a bulk re-date: the ids of the selected claims, their new date (yyyy-MM-dd), and who re-dates them. */
public record BulkRedateRequest(List<Long> claimIds, String date, String by) {
}
