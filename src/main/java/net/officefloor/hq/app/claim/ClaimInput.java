package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A validated claim's fields; a null date means the sheet's today. {@code corrects} is the number of the claim a
 * correction (a negative amount) corrects; null when none was named.
 */
public record ClaimInput(BigDecimal amount, LocalDate date, String note, Integer corrects) {

    public ClaimInput(BigDecimal amount, LocalDate date, String note) {
        this(amount, date, note, null);
    }
}
