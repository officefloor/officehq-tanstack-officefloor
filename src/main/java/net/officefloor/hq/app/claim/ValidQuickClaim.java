package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

/** A quick-add that passed validation: the trimmed name and the amount. */
public record ValidQuickClaim(String name, BigDecimal amount, String user) {
}
