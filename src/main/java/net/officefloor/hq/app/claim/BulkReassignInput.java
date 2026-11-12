package net.officefloor.hq.app.claim;

import java.util.List;

/** A validated bulk move: the selected claim ids, each once (an id that is not a number is null, and is
 *  refused), the person {@code to} move them to (their id), and who moves them. */
public record BulkReassignInput(List<Long> claimIds, long to, String user) {
}
