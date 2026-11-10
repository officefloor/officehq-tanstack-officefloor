package net.officefloor.hq.app.rate;

import java.math.BigDecimal;

/** A validated rate request: its grade and its percent, scaled to two decimals. */
public record RateInput(String grade, BigDecimal pct) {
}
