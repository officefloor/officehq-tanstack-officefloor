package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** A share of a claim: the person holding it and their percent (two decimals). */
public record ClaimShare(long personId, BigDecimal share) {

    /**
     * The holder's part of a settled claim's frozen {@code due}: the due times the share divided by 100, rounded half to
     * even. A settled claim's parts stay as frozen.
     */
    public BigDecimal part(BigDecimal due) {
        return due.multiply(share).movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN);
    }

    /**
     * The holder's part of an unsettled claim: its {@code amount} times the share divided by 100, times {@code rate} (of
     * the holder's own grade in force on the claim's date) divided by 100, rounded by {@code rounding} (the claim's);
     * null when their grade has no rate (it counts as 0.00 against the claim's own person).
     */
    public BigDecimal part(BigDecimal amount, BigDecimal rate, RoundingMode rounding) {
        return rate == null ? null : amount.multiply(share).multiply(rate).movePointLeft(4).setScale(2, rounding);
    }
}
