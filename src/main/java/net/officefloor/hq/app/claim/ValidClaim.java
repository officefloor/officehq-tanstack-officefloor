package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A claim request once checked: the amount to two decimals, the date (null: the sheet's today), the note,
 *  for a correction (a negative amount) the number of the claim it corrects (null otherwise), and its
 *  external reference, trimmed (null: none), and its tags (see {@link ClaimForm#tags}; null: none). */
public record ValidClaim(BigDecimal amount, LocalDate date, String note, String corrects, String ref, String tags) {

    /** Whether it is a correction: its amount is negative. */
    public boolean isCorrection() {
        return amount.signum() < 0;
    }
}
