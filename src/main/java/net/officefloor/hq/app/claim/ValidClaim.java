package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A claim form that passed validation. A null date takes the sheet's today; corrects is the number of
 *  the claim a correction (a negative amount) corrects, null for any other claim;
 *  ref is its external reference (trimmed), null for none. */
public record ValidClaim(BigDecimal amount, LocalDate date, String note, String user, String corrects, String ref) {
}
