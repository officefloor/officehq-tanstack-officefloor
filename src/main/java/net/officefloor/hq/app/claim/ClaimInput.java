package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A validated claim's fields; a null date means the sheet's today. {@code corrects} is the number of the claim a
 * correction (a negative amount) corrects; null when none was named. {@code ref} is the external reference, surrounding
 * spaces removed; null when none was given.
 */
public record ClaimInput(BigDecimal amount, LocalDate date, String note, String corrects, String ref) {

    public ClaimInput(BigDecimal amount, LocalDate date, String note, String corrects) {
        this(amount, date, note, corrects, null);
    }

    public ClaimInput(BigDecimal amount, LocalDate date, String note) {
        this(amount, date, note, null, null);
    }
}
