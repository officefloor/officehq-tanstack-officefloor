package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/** A share of a claim held by another person of the sheet: the holder, their share (a percent, two decimals) and the
 *  sheet's today it was made on. */
public record ClaimShare(long claimId, long personId, BigDecimal share, LocalDate madeOn) {

    /** The holder's part of a claim's due: the due times the share divided by 100, rounded half to even. */
    public BigDecimal part(BigDecimal due) {
        return due.multiply(share).movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN);
    }
}
