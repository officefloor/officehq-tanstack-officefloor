package net.officefloor.hq.app.claim;

import java.math.BigDecimal;

/** A share of a claim once its body is read: who shares it, the person to share it with (null: none given) and their
 *  share (null: not a percent with up to two decimals). */
public record ClaimSharing(String by, Long personId, BigDecimal share) {
}
