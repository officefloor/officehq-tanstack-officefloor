package net.officefloor.hq.app.claim;

import java.util.List;

/** A validated bulk re-date: the selected claim ids (each once) and the re-dating each receives. */
public record BulkRedating(List<Long> claimIds, ClaimRedating redating) {
}
