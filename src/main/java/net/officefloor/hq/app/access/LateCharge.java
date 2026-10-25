package net.officefloor.hq.app.access;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import net.officefloor.hq.app.claim.Claim;

/**
 * The late charge as of the sheet's today: an OPEN (or PENDING) claim past its due-by (fixed when it was added) gains, for every
 * full 30 days it is overdue, the late percent in force on that boundary date (the due-by plus 30, 60, 90 ... days) of its figure,
 * each rounded to two decimals as the claim rounds (see {@link #rounding}), the charges added up. A correction
 * (a negative amount) never gains one. The charge is never bound by a statement; settling freezes it into the settled due.
 * The floor is applied last: a claim's live due is the greater of the floor in force on its date and its figure plus its
 * late charge (a correction has no floor).
 */
public record LateCharge(LocalDate today, DatedSettings settings, LocalDate policyDate) {

    /**
     * How the claim's live figures round: half up when it is dated before the policy date, half to even on or after
     * it. A correction rounds by the date of the claim it corrects.
     */
    public RoundingMode rounding(Claim claim) {
        return claim.getRoundOn().isBefore(policyDate) ? RoundingMode.HALF_UP : RoundingMode.HALF_EVEN;
    }

    /** The claim's late charge on its figure (zero when it is not overdue, is deferred past today, or has no figure). */
    public BigDecimal on(Claim claim, BigDecimal figure) {
        BigDecimal none = BigDecimal.ZERO.setScale(2);
        if (!claim.getStatus().isOpen() || claim.isDeferredOn(today)) {
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
        BigDecimal charge = none;
        for (long n = 1; n <= periods; n++) {
            BigDecimal pct = settings.latePct(claim.getDueBy().plusDays(30 * n));
            if (pct.signum() != 0) {
                charge = charge.add(figure.multiply(pct).movePointLeft(2).setScale(2, rounding(claim)));
            }
        }
        return charge;
    }

    /**
     * The claim's due: its settled due as it is; else the greater of the floor in force on its date and the figure with
     * its late charge added on top (a correction has no floor); null without a figure.
     */
    public BigDecimal due(Claim claim, BigDecimal figure) {
        if (figure == null) {
            return null;
        }
        if (claim.getSettledDue() != null) {
            return figure.setScale(2);
        }
        return floored(claim, figure.setScale(2).add(on(claim, figure)));
    }

    /** The due the claim would have were it OPEN (floored as {@link #due}); null without a figure. */
    public BigDecimal dueAsOpen(Claim claim, BigDecimal figure) {
        return figure == null ? null : floored(claim, figure.setScale(2).add(onAsOpen(claim, figure)));
    }

    /** At least the floor in force on the claim's date, unless it is a correction. */
    private BigDecimal floored(Claim claim, BigDecimal due) {
        return claim.isCorrection() ? due : due.max(settings.floor(claim.getDate()));
    }
}
