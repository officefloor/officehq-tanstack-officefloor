package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

/** A validated claim share: the id of the person it is shared with, their share (two decimals, more than 0.00) and
 *  the user who shares it. */
public record ShareInput(long personId, BigDecimal share, String user) {
}
