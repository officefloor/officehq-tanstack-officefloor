package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

/** The due a claim had fixed before a status change (null: none, or n/a), and whether it was SETTLED then. */
public record PreviousDue(BigDecimal value, boolean settled) {
}
