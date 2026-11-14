package net.officefloor.hq.app.rate;

import java.math.BigDecimal;

/** A rate request that passed validation: a known grade and a percent scaled to two decimals. */
public record ValidRate(String grade, BigDecimal pct) {
}
