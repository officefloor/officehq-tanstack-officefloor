package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * A share of a claim held by another person on the sheet (a percent, two decimals). A live claim's holder's part is
 * worked out by the holder's own grade; a settled claim's parts stay frozen as parts of its fixed due.
 */
public record ClaimShare(long claimId, long personId, BigDecimal share) {

    /** The holder's part of a settled claim's fixed due: the due times the share divided by 100, rounded half to even. */
    public BigDecimal part(BigDecimal due) {
        return due.multiply(share).movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN);
    }

    /**
     * The holder's part of a live claim: its amount times the share divided by 100, times the rate of the holder's
     * grade in force on its date (null: none) divided by 100, rounded by the mode; null with no rate.
     */
    public BigDecimal part(BigDecimal amount, BigDecimal holderRate, RoundingMode mode) {
        return holderRate == null ? null
                : amount.multiply(share).multiply(holderRate).movePointLeft(4).setScale(2, mode);
    }

    /** The claim's own person's part of a due: the due less the holders' parts (n/a, null, taking nothing). */
    public static BigDecimal ownPart(BigDecimal due, List<BigDecimal> parts) {
        BigDecimal own = due;
        for (BigDecimal part : parts) {
            if (part != null) {
                own = own.subtract(part);
            }
        }
        return own;
    }
}
