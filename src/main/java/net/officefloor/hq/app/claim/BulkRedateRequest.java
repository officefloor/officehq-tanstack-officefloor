package net.officefloor.hq.app.claim;

import java.util.List;

/** A bulk re-dating as the screen sends it: the ids of the selected claims and their new date (yyyy-MM-dd). */
public record BulkRedateRequest(List<String> claimIds, String date) {
}
