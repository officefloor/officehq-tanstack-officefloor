package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

/** A split once its body is read: the amount split off the claim (two decimals), and who splits it. */
public record ClaimSplit(BigDecimal amount, String by) {
}
