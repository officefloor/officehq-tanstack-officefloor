package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

/** A validated line on a claim: who is adding it, its label (trimmed, not empty), its quantity (a whole number, at
 *  least 1) and its unit amount (two decimals, not below 0.00). */
public record LineInput(String user, String label, int qty, BigDecimal unit) {
}
