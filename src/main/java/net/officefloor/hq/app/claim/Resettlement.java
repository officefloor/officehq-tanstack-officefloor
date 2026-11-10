package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

/** A claim's re-settle: the due it was settled at before and the due it is settled at now (null for n/a). */
public record Resettlement(BigDecimal old, BigDecimal now) {
}
