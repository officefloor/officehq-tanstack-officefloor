package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

/** A line of a claim: its label (trimmed, not empty), its quantity (a whole number, at least 1) and its
 *  unit amount (two decimals). */
public record ClaimLine(String label, int qty, BigDecimal unit) {

    /** Its quantity times its unit amount. */
    public BigDecimal total() {
        return unit.multiply(BigDecimal.valueOf(qty)).setScale(2);
    }

    /** How it is shown on the claim: {@code <label> <qty> x <unit>}. */
    public String line() {
        return label + " " + qty + " x " + unit.setScale(2).toPlainString();
    }
}
