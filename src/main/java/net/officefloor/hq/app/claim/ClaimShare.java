package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** A share of a claim held by another person of its sheet: their id and the percent of the claim's due they hold
 *  (two decimals). */
public record ClaimShare(long personId, String share) {

    /** A holder's part of a claim's due: the due times their share divided by 100, rounded half to even. */
    public static BigDecimal part(BigDecimal due, String share) {
        return due.multiply(new BigDecimal(share)).movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN);
    }
}
