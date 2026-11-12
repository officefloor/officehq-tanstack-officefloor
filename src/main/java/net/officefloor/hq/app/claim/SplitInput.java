package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

/** A validated split: the amount (two decimals, more than 0.00) to split off the claim and who splits it. */
public record SplitInput(BigDecimal amount, String user) {
}
