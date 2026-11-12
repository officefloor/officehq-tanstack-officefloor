package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated claim add or edit: the date already defaulted to the sheet's today; {@code corrects}
 *  the number of the claim a correction (a negative amount) corrects, null for any other claim. */
public record ClaimInput(BigDecimal amount, LocalDate date, String note, String user, String corrects) {

    public ClaimInput(BigDecimal amount, LocalDate date, String note, String user) {
        this(amount, date, note, user, null);
    }

    /** Whether this is a correction: a negative amount. */
    public boolean isCorrection() {
        return amount.signum() < 0;
    }
}
