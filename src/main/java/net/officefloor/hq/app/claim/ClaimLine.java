package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

/** A validated line for a claim: its label (trimmed, never empty), its quantity (a whole number, at
 *  least 1), its unit amount (two decimals) and who adds it. */
public record ClaimLine(String label, int qty, BigDecimal unit, String user) {
}
