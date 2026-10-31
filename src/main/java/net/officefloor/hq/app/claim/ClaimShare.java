package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/** One holder of a claim: the person holding a share of its due, the share (two decimals) and the sheet's today when shared. */
public record ClaimShare(long personId, BigDecimal share, LocalDate sharedOn) {

    /** The holder's part of a settled claim's frozen due: the due times the share over 100, rounded half to even. */
    public BigDecimal partOf(BigDecimal due) {
        return due.multiply(share).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN);
    }

    /** The holder's part of a live claim: its amount times the share over 100, times the rate of the holder's grade in force
     *  on its date over 100, rounded like the claim; 0.00 when the grade has no rate. */
    public BigDecimal partOf(Claim claim, BigDecimal holderRate, RoundingMode rounding) {
        return holderRate == null ? BigDecimal.ZERO.setScale(2)
                : claim.getAmount().multiply(share).multiply(holderRate).movePointLeft(4).setScale(2, rounding);
    }
}
