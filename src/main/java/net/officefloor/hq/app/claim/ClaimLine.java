package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A line of a claim: who added it, the sheet's today when it was added (null until stored), its label, its quantity
 *  (a whole number, at least 1) and its unit amount (two decimals). */
public record ClaimLine(String by, LocalDate date, String label, int qty, BigDecimal unit) {

    /** The line's quantity times its unit amount. */
    public BigDecimal total() {
        return unit.multiply(BigDecimal.valueOf(qty));
    }
}
