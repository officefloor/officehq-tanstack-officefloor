package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

/** A validated claim share: who is making it, the id of the person taking it and their percent (two decimals, above
 *  0.00). */
public record ShareInput(String user, long personId, BigDecimal share) {
}
