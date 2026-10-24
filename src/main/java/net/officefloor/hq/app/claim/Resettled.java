package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

/** A re-settle's figures: the due the claim had before and the one it was frozen at (null: n/a). */
public record Resettled(BigDecimal old, BigDecimal now) {
}
