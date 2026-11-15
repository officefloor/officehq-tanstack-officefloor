package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** A share of a claim held by another person on the sheet: their id and name, and their percent of its due
 *  (two decimals, as text). */
public record ClaimShare(long personId, String name, String share) {

    /** The holder's part of the claim's due: the due times their share divided by 100, rounded half to even
     *  to two decimals. */
    public BigDecimal partOf(BigDecimal due) {
        return due.multiply(new BigDecimal(share)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN);
    }
}
