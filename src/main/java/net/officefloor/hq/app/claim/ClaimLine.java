package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

/** A line of a claim: a label, a quantity (a whole number, at least 1) and a unit amount (two decimals). */
public record ClaimLine(String label, int qty, BigDecimal unit) {

    /** The line's amount: its quantity times its unit amount. */
    public BigDecimal total() {
        return unit.multiply(BigDecimal.valueOf(qty)).setScale(2);
    }

    /** The line as a claim shows it: "<label> <qty> x <unit>". */
    public String line() {
        return label + " " + qty + " x " + unit.setScale(2).toPlainString();
    }

    /** The lines' amounts added up. */
    public static BigDecimal sum(java.util.List<ClaimLine> lines) {
        return lines.stream().map(ClaimLine::total).reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }
}
