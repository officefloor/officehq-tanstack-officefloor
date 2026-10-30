package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

/** A re-settle: the claim's settled due before and after; null for n/a. */
public record ClaimResettle(BigDecimal oldDue, BigDecimal newDue) {
}
