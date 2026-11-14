package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** A share of a claim held by another person on the sheet: a percent of the claim's due (two decimals). */
public record ClaimShare(long claimId, long personId, BigDecimal share) {

    /** The holder's part of a due: the due times the share divided by 100, rounded half to even. */
    public BigDecimal part(BigDecimal due) {
        return due.multiply(share).movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN);
    }

    /** The claim's own person's part of a due: the due less the holders' parts. */
    public static BigDecimal ownPart(BigDecimal due, List<ClaimShare> shares) {
        BigDecimal own = due;
        for (ClaimShare s : shares) {
            own = own.subtract(s.part(due));
        }
        return own;
    }
}
