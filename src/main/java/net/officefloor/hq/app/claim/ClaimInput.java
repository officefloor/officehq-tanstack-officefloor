package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated claim add or edit: the date already defaulted to the sheet's today; {@code corrects}
 *  the number of the claim a correction (a negative amount) corrects, null for any other claim;
 *  {@code ref} the external reference, spaces around it removed, null when none was given; {@code tags} its
 *  tags (see {@link Tags}), null when none were given. */
public record ClaimInput(BigDecimal amount, LocalDate date, String note, String user, String corrects, String ref,
        String tags) {

    public ClaimInput(BigDecimal amount, LocalDate date, String note, String user, String corrects, String ref) {
        this(amount, date, note, user, corrects, ref, null);
    }

    public ClaimInput(BigDecimal amount, LocalDate date, String note, String user, String corrects) {
        this(amount, date, note, user, corrects, null);
    }

    public ClaimInput(BigDecimal amount, LocalDate date, String note, String user) {
        this(amount, date, note, user, null);
    }

    /** Whether this is a correction: a negative amount. */
    public boolean isCorrection() {
        return amount.signum() < 0;
    }
}
