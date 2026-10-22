package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

/** A validated split: who is making it and the amount (two decimals, above 0.00) to split off the claim. */
public record SplitInput(String user, BigDecimal amount) {
}
