package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

/** A re-settled claim's settled due before and after (null for n/a). */
public record Resettled(BigDecimal before, BigDecimal after) {
}
