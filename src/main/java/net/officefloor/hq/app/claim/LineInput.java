package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

/** A checked claim line: its trimmed label, its quantity (at least 1) and its unit amount (two decimals). */
public record LineInput(String label, int qty, BigDecimal unit) {

    /** The line as the audit records it: label=<label> qty=<qty> unit=<unit>. */
    public String audit() {
        return "label=" + label + " qty=" + qty + " unit=" + unit.setScale(2).toPlainString();
    }
}
