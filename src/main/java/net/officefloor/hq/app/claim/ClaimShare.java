package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** One share of a claim: the person on the sheet holding it, and their share of its due (a percent, two decimals). */
public record ClaimShare(long person, String share) {

    /** The holder's part of the claim's due: the due times the share divided by 100, rounded half to even to two decimals. */
    public BigDecimal partOf(BigDecimal due) {
        return due.multiply(new BigDecimal(share)).movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN);
    }
}
