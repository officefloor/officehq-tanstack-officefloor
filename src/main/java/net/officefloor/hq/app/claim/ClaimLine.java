package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

/** A line of a claim: its label (trimmed), its quantity (at least 1) and its unit amount (two decimals). */
public record ClaimLine(String label, int qty, BigDecimal unit) {

    /** The line's amount: its quantity times its unit amount. */
    public BigDecimal amount() {
        return unit.multiply(BigDecimal.valueOf(qty));
    }

    /** The line as the screen shows it: "label qty x unit". */
    public String shown() {
        return label + " " + qty + " x " + unit.toPlainString();
    }
}
