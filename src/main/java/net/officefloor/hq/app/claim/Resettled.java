package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

/** A re-settle: the claim's frozen due before ({@code old}) and after ({@code now}); null for n/a. */
public record Resettled(BigDecimal old, BigDecimal now) {
}
