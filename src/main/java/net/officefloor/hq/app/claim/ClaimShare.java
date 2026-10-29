package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** A share of a claim: the person holding it and their percent (two decimals). */
public record ClaimShare(long personId, BigDecimal share) {

    /** The holder's part of the claim's {@code due}: the due times the share divided by 100, rounded half to even. */
    public BigDecimal part(BigDecimal due) {
        return due.multiply(share).movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN);
    }
}
