package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/** A share of a claim held by another person of the sheet: the holder, their share (a percent, two decimals) and the
 *  sheet's today it was made on. */
public record ClaimShare(long claimId, long personId, BigDecimal share, LocalDate madeOn) {

    /** The holder's part of a settled claim's due, frozen with it: the due times the share divided by 100, rounded half
     *  to even. */
    public BigDecimal part(BigDecimal due) {
        return due.multiply(share).movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN);
    }

    /** The holder's part of a claim not settled: its amount times the share divided by 100, times the rate of the
     *  holder's own grade in force on its date (null: none) divided by 100, rounded like the claim; 0.00 with no rate. */
    public BigDecimal part(BigDecimal amount, BigDecimal rate, RoundingMode rounding) {
        if (rate == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        return amount.multiply(share).multiply(rate).movePointLeft(4).setScale(2, rounding);
    }
}
