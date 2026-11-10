package net.officefloor.hq.app.claim;

import java.util.List;

/** A bulk settle as the screen sends it: the ids of the selected claims. */
public record BulkSettleRequest(List<String> claimIds) {
}
