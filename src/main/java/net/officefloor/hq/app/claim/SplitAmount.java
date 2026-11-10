package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

/** A split, read from its request: the amount to split off the claim. */
public record SplitAmount(BigDecimal amount) {
}
