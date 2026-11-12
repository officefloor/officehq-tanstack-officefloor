package net.officefloor.hq.app.claim;

import java.util.List;

/** A validated bulk open: the selected claim ids, each once (an id that is not a number is
 *  null, and is refused), and who opens them. */
public record BulkOpenInput(List<Long> claimIds, String user) {
}
