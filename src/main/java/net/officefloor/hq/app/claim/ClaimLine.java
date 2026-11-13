package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

/** A line of a DRAFT claim: a label, a quantity (a whole number, at least 1) and a unit amount (two decimals). */
public record ClaimLine(long claimId, String label, int qty, BigDecimal unit) {

    /** The line's quantity times its unit amount. */
    public BigDecimal total() {
        return unit.multiply(BigDecimal.valueOf(qty)).setScale(2);
    }
}
