package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** A share of a claim held by another person on the sheet: their id and name, and their percent of its due
 *  (two decimals, as text). */
public record ClaimShare(long personId, String name, String share) {

    /** The holder's frozen part of a settled claim's due: the due times their share divided by 100, rounded half to even
     *  to two decimals. */
    public BigDecimal partOf(BigDecimal due) {
        return due.multiply(new BigDecimal(share)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN);
    }

    /** A holder's part of a live claim: the claim's amount times their share divided by 100, times the rate
     *  of their own grade ({@code pct}; null: none, the part is 0.00) divided by 100, rounded ({@code rounding})
     *  to two decimals like the claim. */
    public static BigDecimal partOf(String amount, String share, String pct, RoundingMode rounding) {
        if (pct == null) return BigDecimal.ZERO.setScale(2);
        return new BigDecimal(amount).multiply(new BigDecimal(share)).multiply(new BigDecimal(pct))
                .divide(BigDecimal.valueOf(10000), 2, rounding);
    }
}
