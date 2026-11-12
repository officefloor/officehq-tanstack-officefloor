package net.officefloor.hq.app.claim;

import java.util.List;

/** A validated bulk void: the selected claim ids, each once (an id that is not a number is
 *  null, and is refused), who voids them and the trimmed void reason ("" for none). */
public record BulkVoidInput(List<Long> claimIds, String user, String reason) {
}
