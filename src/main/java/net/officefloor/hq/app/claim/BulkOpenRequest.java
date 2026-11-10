package net.officefloor.hq.app.claim;

import java.util.List;

/** A bulk open as the screen sends it: the ids of the selected claims. */
public record BulkOpenRequest(List<String> claimIds) {
}
