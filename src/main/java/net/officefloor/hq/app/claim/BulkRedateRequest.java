package net.officefloor.hq.app.claim;

import java.util.List;

/** The bulk re-date: the ids of the selected claims and the new date they all take. */
public record BulkRedateRequest(List<Long> claimIds, String date) {
}
