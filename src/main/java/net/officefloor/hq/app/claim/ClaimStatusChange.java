package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated status change body; `reason` is a rejection's reason, or the reason a void
 *  was given from the sheet's list (null when none, and for any other action). A settle as of a
 *  date carries the date (`on`) and the claim's due as it stood at the end of it (`onDue`, null
 *  for n/a); both are null for any other change. */
public record ClaimStatusChange(ClaimAction action, String user, String reason, LocalDate on, BigDecimal onDue) {
    public ClaimStatusChange(ClaimAction action, String user, String reason) {
        this(action, user, reason, null, null);
    }

    public ClaimStatusChange(ClaimAction action, String user) {
        this(action, user, null);
    }
}
