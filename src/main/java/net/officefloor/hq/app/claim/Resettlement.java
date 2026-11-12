package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

/** A stored re-settle: the settled due the claim had before and the one it has now (null for n/a). */
public record Resettlement(BigDecimal old, BigDecimal now) {
}
