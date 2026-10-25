package net.officefloor.hq.app.access;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimStatus;

/**
 * The late charge as of the sheet's today: an OPEN claim past its due-by (its date plus the terms) gains the late
 * percent of its figure for every full 30 days it is overdue, rounded to two decimals as the claim rounds (see
 * {@link #rounding}). A correction
 * (a negative amount) never gains one. The charge is never bound by a statement; settling freezes it into the settled due.
 */
public record LateCharge(LocalDate today, int terms, BigDecimal pct, LocalDate policyDate) {

    /**
     * How the claim's live figures round: half up when it is dated before the policy date, half to even on or after
     * it. A correction rounds by the date of the claim it corrects.
     */
    public RoundingMode rounding(Claim claim) {
        return claim.getRoundOn().isBefore(policyDate) ? RoundingMode.HALF_UP : RoundingMode.HALF_EVEN;
    }

    /** The claim's late charge on its figure (zero when it is not overdue, or has no figure). */
    public BigDecimal on(Claim claim, BigDecimal figure) {
        BigDecimal none = BigDecimal.ZERO.setScale(2);
        if (figure == null || claim.getStatus() != ClaimStatus.OPEN || claim.isCorrection()) {
            return none;
        }
        long overdue = ChronoUnit.DAYS.between(claim.getDate().plusDays(terms), today);
        long periods = overdue > 0 ? overdue / 30 : 0;
        if (periods == 0) {
            return none;
        }
        return figure.multiply(pct).multiply(BigDecimal.valueOf(periods)).movePointLeft(2)
                .setScale(2, rounding(claim));
    }

    /** The figure with the claim's late charge added on top; null without a figure. */
    public BigDecimal due(Claim claim, BigDecimal figure) {
        return figure == null ? null : figure.setScale(2).add(on(claim, figure));
    }
}
