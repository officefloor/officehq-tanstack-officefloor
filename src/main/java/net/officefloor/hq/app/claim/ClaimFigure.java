package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import net.officefloor.hq.app.rate.Rate;
import net.officefloor.hq.app.setting.Settings;

/**
 * A claim's due figure, as the screen shows it: the due fixed when it was settled, else the figure
 * its person's last statement bound it to (never while it is PENDING), else its live due: its amount times its grade's rate in
 * force on its date, divided by 100 and rounded to two decimals, raised to the sheet's floor when
 * below it (a correction, a negative amount, has no floor and no late charge). Null for n/a (no
 * floor applies to n/a). Every rounding of a claim's live figures is by the sheet's policy date
 * ({@link Settings#rounding}): half up when the claim is dated before it, half to even on or after;
 * a correction rounds by the date of the claim it corrects.
 */
public final class ClaimFigure {
    private ClaimFigure() {}

    public static BigDecimal of(Claim claim, List<Rate> rates, String grade, Settings settings, LocalDate today) {
        if (claim.getSettledDue() != null) return claim.getSettledDue();
        if (claim.isBound()) return claim.getStatedDue();
        return live(claim, rates, grade, settings, today);
    }

    /** The claim's live due at its grade's rate on its date; a correction has no floor. */
    public static BigDecimal live(Claim claim, List<Rate> rates, String grade, Settings settings, LocalDate today) {
        RoundingMode mode = rounding(claim, settings, today);
        return Rate.inForce(rates, grade, claim.getDate())
                .map((pct) -> times(claim.getAmount(), pct, mode))
                .map((due) -> claim.isCorrection() ? due : due.max(settings.floorOn(claim.getDate())))
                .orElse(null);
    }

    /** How the claim's live figures round, by the date it rounds by and the sheet's policy date. */
    public static RoundingMode rounding(Claim claim, Settings settings, LocalDate today) {
        return settings.rounding(claim.getRoundDate(), today);
    }

    /**
     * The claim's due with its late charge on top: its figure ({@link #of}) plus the late charge
     * an OPEN or PENDING claim gains while overdue on {@code today}. Null for n/a.
     */
    public static BigDecimal withLate(Claim claim, List<Rate> rates, String grade, Settings settings,
            LocalDate today) {
        BigDecimal figure = of(claim, rates, grade, settings, today);
        return figure == null ? null : figure.add(late(claim, figure, settings, today));
    }

    /**
     * The late charge on a figure: for an OPEN or PENDING claim past its due-by (fixed when it was made)
     * on {@code today}, the sheet's late percent of the figure for every full 30 days it is
     * overdue, rounded to two decimals as the claim rounds; else 0.00 (as for a claim deferred on today). Never bound by a statement:
     * it follows the sheet's today until the claim is settled.
     */
    public static BigDecimal late(Claim claim, BigDecimal figure, Settings settings, LocalDate today) {
        if (!claim.isOpen() || claim.isDeferredOn(today)) return BigDecimal.ZERO.setScale(2);
        return lateAsOpen(claim, figure, settings, today);
    }

    /** The late charge on a figure as {@link #late} reads it for an open claim, whatever the claim's status. */
    public static BigDecimal lateAsOpen(Claim claim, BigDecimal figure, Settings settings, LocalDate today) {
        BigDecimal none = BigDecimal.ZERO.setScale(2);
        if (figure == null || claim.isCorrection()) return none;
        long overdue = ChronoUnit.DAYS.between(claim.getDueBy(), today);
        long periods = overdue > 0 ? overdue / 30 : 0;
        if (periods == 0) return none;
        return figure.multiply(settings.latePctOn(claim.getDate())).multiply(BigDecimal.valueOf(periods))
                .divide(BigDecimal.valueOf(100), 2, rounding(claim, settings, today));
    }

    /** The amount times the percent, divided by 100 and rounded to two decimals. */
    private static BigDecimal times(BigDecimal amount, BigDecimal pct, RoundingMode mode) {
        return amount.multiply(pct).divide(BigDecimal.valueOf(100), 2, mode);
    }
}
