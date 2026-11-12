package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

/** A validated claim line: who adds it, its label (trimmed, not empty), its quantity (a whole number, at least 1)
 *  and its unit amount (two decimals). */
public record LineInput(String user, String label, int qty, BigDecimal unit) {
}
