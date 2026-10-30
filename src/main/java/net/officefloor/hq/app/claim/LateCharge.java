package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import net.officefloor.hq.app.rate.Rates;

/**
 * The late charge on a claim: an OPEN (or PENDING) claim whose due-by (fixed when it was created) is before
 * the sheet's today gains the sheet's late percent of its figure for every full 30 days it is
 * overdue, rounded like due. A correction has no late charge. The charge is never bound by a statement; settling freezes it in.
 */
public final class LateCharge {

    public static final int PERIOD_DAYS = 30;

    private LateCharge() {
    }

    /** The charge on a claim's figure; zero when it is not overdue or has no figure. */
    public static BigDecimal of(Claim claim, BigDecimal figure, BigDecimal pct, LocalDate today,
            RoundingMode rounding) {
        if (!claim.getStatus().isOpen()) {
            return BigDecimal.ZERO.setScale(2);
        }
        return asOpen(claim, figure, pct, today, rounding);
    }

    /** The charge the claim's figure would carry were it open, whatever its status; re-settling uses it. */
    public static BigDecimal asOpen(Claim claim, BigDecimal figure, BigDecimal pct, LocalDate today,
            RoundingMode rounding) {
        if (figure == null || pct == null || pct.signum() == 0 || claim.isCorrection()) {
            return BigDecimal.ZERO.setScale(2);
        }
        long overdue = ChronoUnit.DAYS.between(claim.getDueBy(), today);
        long periods = overdue > 0 ? overdue / PERIOD_DAYS : 0;
        return Rates.due(figure.multiply(BigDecimal.valueOf(periods)), pct, rounding);
    }

    /** The figure with its late charge added on top; null stays null. */
    public static BigDecimal withCharge(Claim claim, BigDecimal figure, BigDecimal pct, LocalDate today,
            RoundingMode rounding) {
        return figure == null ? null : figure.add(of(claim, figure, pct, today, rounding));
    }
}
