package net.officefloor.hq.app.access;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import net.officefloor.hq.app.claim.Claim;

/**
 * The late charge as of the sheet's today: an OPEN (or PENDING) claim past its due-by (fixed when it was added) gains the late
 * percent in force on its date of its figure for every full 30 days it is overdue, rounded to two decimals as the claim rounds (see
 * {@link #rounding}). A correction
 * (a negative amount) never gains one. The charge is never bound by a statement; settling freezes it into the settled due.
 */
public record LateCharge(LocalDate today, DatedSettings settings, LocalDate policyDate) {

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
        if (!claim.getStatus().isOpen()) {
            return none;
        }
        return onAsOpen(claim, figure);
    }

    /** The late charge the claim would have on its figure were it OPEN. */
    public BigDecimal onAsOpen(Claim claim, BigDecimal figure) {
        BigDecimal none = BigDecimal.ZERO.setScale(2);
        if (figure == null || claim.isCorrection()) {
            return none;
        }
        long overdue = ChronoUnit.DAYS.between(claim.getDueBy(), today);
        long periods = overdue > 0 ? overdue / 30 : 0;
        if (periods == 0) {
            return none;
        }
        return figure.multiply(settings.latePct(claim.getDate())).multiply(BigDecimal.valueOf(periods)).movePointLeft(2)
                .setScale(2, rounding(claim));
    }

    /** The figure with the claim's late charge added on top; null without a figure. */
    public BigDecimal due(Claim claim, BigDecimal figure) {
        return figure == null ? null : figure.setScale(2).add(on(claim, figure));
    }

    /** The figure with the late charge the claim would have were it OPEN; null without a figure. */
    public BigDecimal dueAsOpen(Claim claim, BigDecimal figure) {
        return figure == null ? null : figure.setScale(2).add(onAsOpen(claim, figure));
    }
}
