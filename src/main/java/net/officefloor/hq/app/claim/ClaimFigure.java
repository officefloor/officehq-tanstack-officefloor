package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.setting.Settings;

/**
 * A claim's figures, as the screen shows them. Its rate figure: the figure its person's last
 * statement bound it to (never while it is PENDING), else its amount times its grade's rate in
 * force on its date (of the rates that apply to it: a forward-only rate only to claims added after it
 * was saved), divided by 100 and rounded to two decimals. Its live due: the greater of the
 * sheet's floor in force on its date and its rate figure plus its late charge (worked out on the
 * rate figure alone); a correction, a negative amount, has no floor and no late charge. A settled
 * claim's due stays as it was fixed. Null for n/a (no floor applies to n/a). Every rounding of a
 * claim's live figures is by the sheet's policy date ({@link Settings#rounding}): half up when the
 * claim is dated before it, half to even on or after; a correction rounds by the date of the claim
 * it corrects.
 */
public final class ClaimFigure {
    private ClaimFigure() {}

    /** The claim's rate figure: its bound figure when a statement bound it, else its live rate figure. */
    public static BigDecimal of(Claim claim, List<Rate> rates, String grade, Settings settings, LocalDate today) {
        if (claim.isBound()) return claim.getStatedDue();
        return live(claim, rates, grade, settings, today);
    }

    /** The claim's amount at its grade's rate on its date, before any floor or late charge. */
    public static BigDecimal live(Claim claim, List<Rate> rates, String grade, Settings settings, LocalDate today) {
        RoundingMode mode = rounding(claim, settings, today);
        return Rate.inForce(rates, grade, claim.getDate(), claim.getId())
                .map((pct) -> times(claim.getAmount(), pct, mode))
                .orElse(null);
    }

    /**
     * The due on a rate figure with a late charge: the greater of the floor in force on the claim's
     * date and the figure plus the charge; a correction has no floor. Null for n/a.
     */
    public static BigDecimal floored(Claim claim, BigDecimal figure, BigDecimal late, Settings settings) {
        if (figure == null) return null;
        BigDecimal due = figure.add(late);
        return claim.isCorrection() ? due : due.max(settings.floorOn(claim.getDate()));
    }

    /** How the claim's live figures round, by the date it rounds by and the sheet's policy date. */
    public static RoundingMode rounding(Claim claim, Settings settings, LocalDate today) {
        return settings.rounding(claim.getRoundDate(), today);
    }

    /**
     * The claim's due as the screen shows it: the due fixed when it was settled, else its live due
     * ({@link #floored}) on its rate figure ({@link #of}) and the late charge an OPEN or PENDING claim
     * gains on that figure while overdue on {@code today}. Null for n/a.
     */
    public static BigDecimal withLate(Claim claim, List<Rate> rates, String grade, Settings settings,
            LocalDate today) {
        if (claim.getSettledDue() != null) return claim.getSettledDue();
        BigDecimal figure = of(claim, rates, grade, settings, today);
        return figure == null ? null : floored(claim, figure, late(claim, figure, grade, settings, today), settings);
    }

    /**
     * The late charge on a figure: for an OPEN or PENDING claim past its due-by (fixed when it was made)
     * plus the grace days of its grade ({@code grade}, the one in force on its date) on its date
     * ({@link Settings#overdueFrom}) on {@code today}, for every full 30 days it is overdue the
     * sheet's late percent in force on that boundary date (that day plus 30, 60, 90 ... days) of the figure plus the
     * charges of the boundaries before it (compounding), each rounded to two
     * decimals as the claim rounds, added up; else 0.00 (as for a claim deferred on today). Never bound by a statement:
     * it follows the sheet's today until the claim is settled.
     */
    public static BigDecimal late(Claim claim, BigDecimal figure, String grade, Settings settings, LocalDate today) {
        if (!claim.isOpen() || claim.isDeferredOn(today)) return BigDecimal.ZERO.setScale(2);
        return lateAsOpen(claim, figure, grade, settings, today);
    }

    /** The late charge on a figure as {@link #late} reads it for an open claim, whatever the claim's status. */
    public static BigDecimal lateAsOpen(Claim claim, BigDecimal figure, String grade, Settings settings,
            LocalDate today) {
        BigDecimal none = BigDecimal.ZERO.setScale(2);
        if (figure == null || claim.isCorrection()) return none;
        LocalDate from = settings.overdueFrom(claim.getDueBy(), grade, claim.getDate());
        long overdue = ChronoUnit.DAYS.between(from, today);
        long periods = overdue > 0 ? overdue / 30 : 0;
        RoundingMode mode = rounding(claim, settings, today);
        BigDecimal charge = none;
        for (long k = 1; k <= periods; k++) {
            BigDecimal pct = settings.latePctOn(from.plusDays(30 * k));
            if (pct.signum() != 0) charge = charge.add(times(figure.add(charge), pct, mode));
        }
        return charge;
    }

    /** The amount times the percent, divided by 100 and rounded to two decimals. */
    private static BigDecimal times(BigDecimal amount, BigDecimal pct, RoundingMode mode) {
        return amount.multiply(pct).divide(BigDecimal.valueOf(100), 2, mode);
    }
}
