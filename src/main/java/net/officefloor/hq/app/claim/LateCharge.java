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
 * boundary date (its due-by plus 30, 60, 90 ... days) of its figure plus the charges of the boundaries before it
 * (compounding), each rounded like due, the charges added up
 * (a boundary whose percent is 0.00 adds nothing); a deferred claim is not overdue. A correction has no late charge. The charge is never bound by a statement; settling freezes it in.
 * The charge is worked on the rate figure alone (or the figure a statement bound); the floor is applied last.
 * With grace days (those of its person's grade in force on its date) the claim is overdue only once its due-by plus them is
 * before today, and the 30-day boundaries are counted from its due-by plus them.
 */
public final class LateCharge {

    public static final int PERIOD_DAYS = 30;

    private LateCharge() {
    }

    /** The charge on a claim's figure; zero when it is not overdue or has no figure. */
    public static BigDecimal of(Claim claim, BigDecimal figure, Function<LocalDate, BigDecimal> pct, LocalDate today,
            RoundingMode rounding) {
        return of(claim, figure, pct, today, rounding, 0);
    }

    /** As {@link #of(Claim, BigDecimal, Function, LocalDate, RoundingMode)}, with the claim's grace days. */
    public static BigDecimal of(Claim claim, BigDecimal figure, Function<LocalDate, BigDecimal> pct, LocalDate today,
            RoundingMode rounding, int grace) {
        if (!claim.getStatus().isOpen() || claim.isDeferredOn(today)) {
            return BigDecimal.ZERO.setScale(2);
        }
        return asOpen(claim, figure, pct, today, rounding, grace);
    }

    /** The charge the claim's figure would carry were it open, whatever its status; re-settling uses it. */
    public static BigDecimal asOpen(Claim claim, BigDecimal figure, Function<LocalDate, BigDecimal> pct, LocalDate today,
            RoundingMode rounding) {
        return asOpen(claim, figure, pct, today, rounding, 0);
    }

    /** As {@link #asOpen(Claim, BigDecimal, Function, LocalDate, RoundingMode)}, with the claim's grace days. */
    public static BigDecimal asOpen(Claim claim, BigDecimal figure, Function<LocalDate, BigDecimal> pct, LocalDate today,
            RoundingMode rounding, int grace) {
        BigDecimal charge = BigDecimal.ZERO.setScale(2);
        if (figure == null || claim.isCorrection()) {
            return charge;
        }
        LocalDate lateFrom = lateFrom(claim, grace);
        long overdue = ChronoUnit.DAYS.between(lateFrom, today);
        long periods = overdue > 0 ? overdue / PERIOD_DAYS : 0;
        for (long n = 1; n <= periods; n++) {
            BigDecimal rate = pct.apply(lateFrom.plusDays(n * PERIOD_DAYS));
            if (rate != null && rate.signum() != 0) {
                charge = charge.add(Rates.due(figure.add(charge), rate, rounding));
            }
        }
        return charge;
    }

    /** A claim's due from its figure (the rate figure, or the one a statement bound): the greater of the floor in force on its
     *  date and the figure with its late charge on top. A settled due is frozen as it is; a correction has no floor and no
     *  charge; null stays null. */
    public static BigDecimal due(Claim claim, BigDecimal figure, BigDecimal floor, Function<LocalDate, BigDecimal> pct,
            LocalDate today, RoundingMode rounding) {
        return due(claim, figure, floor, pct, today, rounding, 0);
    }

    /** As {@link #due(Claim, BigDecimal, BigDecimal, Function, LocalDate, RoundingMode)}, with the claim's grace days. */
    public static BigDecimal due(Claim claim, BigDecimal figure, BigDecimal floor, Function<LocalDate, BigDecimal> pct,
            LocalDate today, RoundingMode rounding, int grace) {
        if (figure == null || claim.getSettledDue() != null) {
            return figure;
        }
        return floored(claim, figure.add(of(claim, figure, pct, today, rounding, grace)), floor);
    }

    /** The date a claim's lateness counts from: its due-by plus its grace days. */
    public static LocalDate lateFrom(Claim claim, int grace) {
        return claim.getDueBy() == null ? null : claim.getDueBy().plusDays(grace);
    }

    /** A due raised to the floor; a correction has no floor. */
    public static BigDecimal floored(Claim claim, BigDecimal due, BigDecimal floor) {
        return due == null || floor == null || claim.isCorrection() || due.compareTo(floor) >= 0 ? due : floor.setScale(2);
    }
}
