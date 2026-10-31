package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/** One holder of a claim: the person holding a share of its due, the share (two decimals) and the sheet's today when shared. */
public record ClaimShare(long personId, BigDecimal share, LocalDate sharedOn) {

    /** The holder's part of a due: the due times the share over 100, rounded half to even. */
    public BigDecimal partOf(BigDecimal due) {
        return due.multiply(share).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN);
    }
}
