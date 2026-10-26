package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

/** A claim's line, as validated: its trimmed label, a whole quantity of 1 or more and a unit amount (two decimals). */
public record ClaimLine(String label, int qty, BigDecimal unit) {

    /** The line's quantity times its unit amount. */
    public BigDecimal total() {
        return unit.multiply(BigDecimal.valueOf(qty)).setScale(2);
    }
}
