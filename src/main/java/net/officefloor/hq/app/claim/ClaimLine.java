package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

/** A validated claim line: a trimmed label, a whole quantity of at least 1 and a unit amount with two decimals. */
public record ClaimLine(String label, int qty, BigDecimal unit) {
    /** The line's amount: its quantity times its unit amount. */
    public BigDecimal amount() {
        return unit.multiply(BigDecimal.valueOf(qty));
    }
}
