package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

/** The validated amount a split takes off its claim, with two decimals. */
public record SplitAmount(BigDecimal value) {
}
