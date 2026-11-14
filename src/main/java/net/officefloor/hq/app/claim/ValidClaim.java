package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A claim request once checked: the amount to two decimals, the date (null: the sheet's today), the note,
 *  and for a correction (a negative amount) the number of the claim it corrects (null otherwise). */
public record ValidClaim(BigDecimal amount, LocalDate date, String note, String corrects) {

    /** Whether it is a correction: its amount is negative. */
    public boolean isCorrection() {
        return amount.signum() < 0;
    }
}
