package net.officefloor.hq.app.claim;

import java.util.List;

/** Body of a bulk re-dating, as sent: the ids of the selected claims, their new date (yyyy-MM-dd) and who
 *  re-dates them. */
public record BulkRedateRequest(List<String> claimIds, String date, String user) {
}
