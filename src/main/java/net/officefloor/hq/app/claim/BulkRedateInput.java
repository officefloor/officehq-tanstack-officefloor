package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.List;

/** A validated bulk re-dating: the selected claim ids, each once (an id that is not a number is null, and
 *  is refused), their new date and who re-dates them. */
public record BulkRedateInput(List<Long> claimIds, LocalDate date, String user) {
}
