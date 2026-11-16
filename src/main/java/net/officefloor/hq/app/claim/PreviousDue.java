package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

/** The due a claim had fixed before a status change (null: none, or n/a), so a re-settle can name its old figure. */
public record PreviousDue(BigDecimal value) {
}
