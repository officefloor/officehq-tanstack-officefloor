package net.officefloor.hq.app.claim;

import java.util.List;

/** A bulk void as the screen sends it: the ids of the selected claims and the reason chosen ('' or null: none). */
public record BulkVoidRequest(List<String> claimIds, String reason) {
}
