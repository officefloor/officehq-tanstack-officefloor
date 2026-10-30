package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

/** A split with its amount parsed. */
public record ValidSplit(String user, BigDecimal amount) {
}
