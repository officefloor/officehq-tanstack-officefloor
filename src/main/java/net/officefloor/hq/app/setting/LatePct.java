package net.officefloor.hq.app.setting;

import java.math.BigDecimal;

/** A validated late percent with two decimals; {@code value} null: left as it is. */
public record LatePct(BigDecimal value) {
}
