package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;
import net.officefloor.hq.app.rate.Rates;

/**
 * The late charge on a claim: an OPEN (or PENDING) claim whose due-by (fixed when it was created) is before
 * the sheet's today gains, for each full 30 days it is overdue, the sheet's late percent in force on that
 * boundary date (its due-by plus 30, 60, 90 ... days) of its figure, rounded like due, the charges added up
 * (a boundary whose percent is 0.00 adds nothing); a deferred claim is not overdue. A correction has no late charge. The charge is never bound by a statement; settling freezes it in.
 * The charge is worked on the rate figure alone (or the figure a statement bound); the floor is applied last.
 */
public final class LateCharge {

    public static final int PERIOD_DAYS = 30;

    private LateCharge() {
    }

    /** The charge on a claim's figure; zero when it is not overdue or has no figure. */
    public static BigDecimal of(Claim claim, BigDecimal figure, Function<LocalDate, BigDecimal> pct, LocalDate today,
            RoundingMode rounding) {
        if (!claim.getStatus().isOpen() || claim.isDeferredOn(today)) {
            return BigDecimal.ZERO.setScale(2);
        }
        return asOpen(claim, figure, pct, today, rounding);
    }

    /** The charge the claim's figure would carry were it open, whatever its status; re-settling uses it. */
    public static BigDecimal asOpen(Claim claim, BigDecimal figure, Function<LocalDate, BigDecimal> pct, LocalDate today,
            RoundingMode rounding) {
        BigDecimal charge = BigDecimal.ZERO.setScale(2);
        if (figure == null || claim.isCorrection()) {
            return charge;
        }
        long overdue = ChronoUnit.DAYS.between(claim.getDueBy(), today);
        long periods = overdue > 0 ? overdue / PERIOD_DAYS : 0;
        for (long n = 1; n <= periods; n++) {
            BigDecimal rate = pct.apply(claim.getDueBy().plusDays(n * PERIOD_DAYS));
            if (rate != null && rate.signum() != 0) {
                charge = charge.add(Rates.due(figure, rate, rounding));
            }
        }
        return charge;
    }

    /** A claim's due from its figure (the rate figure, or the one a statement bound): the greater of the floor in force on its
     *  date and the figure with its late charge on top. A settled due is frozen as it is; a correction has no floor and no
     *  charge; null stays null. */
    public static BigDecimal due(Claim claim, BigDecimal figure, BigDecimal floor, Function<LocalDate, BigDecimal> pct,
            LocalDate today, RoundingMode rounding) {
        if (figure == null || claim.getSettledDue() != null) {
            return figure;
        }
        return floored(claim, figure.add(of(claim, figure, pct, today, rounding)), floor);
    }

    /** A due raised to the floor; a correction has no floor. */
    public static BigDecimal floored(Claim claim, BigDecimal due, BigDecimal floor) {
        return due == null || floor == null || claim.isCorrection() || due.compareTo(floor) >= 0 ? due : floor.setScale(2);
    }
}
