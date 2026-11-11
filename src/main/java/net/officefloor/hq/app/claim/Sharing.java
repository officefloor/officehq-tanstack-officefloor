package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

/** A share to add, validated: the holder's person id and their percent, two decimals, more than 0.00. */
public record Sharing(long personId, BigDecimal share) {
}
