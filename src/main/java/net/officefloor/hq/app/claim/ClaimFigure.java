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
 * its person's last statement bound it to, else its live due: its amount times its grade's rate in
 * force on its date, divided by 100 and rounded half to even to two decimals, raised to the sheet's
 * floor when below it. Null for n/a (no floor applies to n/a).
 */
public final class ClaimFigure {
    private ClaimFigure() {}

    public static BigDecimal of(Claim claim, List<Rate> rates, String grade, BigDecimal floor) {
        if (claim.getSettledDue() != null) return claim.getSettledDue();
        if (claim.isStated()) return claim.getStatedDue();
        return live(claim.getAmount(), rates, grade, claim.getDate(), floor);
    }

    /**
     * The claim's due with its late charge on top: its figure ({@link #of}) plus the late charge
     * an OPEN claim gains while overdue on {@code today}. Null for n/a.
     */
    public static BigDecimal withLate(Claim claim, List<Rate> rates, String grade, Settings settings,
            LocalDate today) {
        BigDecimal figure = of(claim, rates, grade, settings.floor());
        return figure == null ? null : figure.add(late(claim, figure, settings, today));
    }

    /**
     * The late charge on a figure: for an OPEN claim past its due-by (its date plus the sheet's
     * terms) on {@code today}, the sheet's late percent of the figure for every full 30 days it is
     * overdue, rounded half to even to two decimals; else 0.00. Never bound by a statement: it
     * follows the sheet's today until the claim is settled.
     */
    public static BigDecimal late(Claim claim, BigDecimal figure, Settings settings, LocalDate today) {
        BigDecimal none = BigDecimal.ZERO.setScale(2);
        if (claim.getStatus() != ClaimStatus.OPEN || figure == null) return none;
        long overdue = ChronoUnit.DAYS.between(claim.getDate().plusDays(settings.terms()), today);
        long periods = overdue > 0 ? overdue / 30 : 0;
        if (periods == 0) return none;
        return figure.multiply(settings.latePct()).multiply(BigDecimal.valueOf(periods))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN);
    }

    /** The amount at the grade's rate in force on the date, at least the floor; null with no such rate. */
    public static BigDecimal live(BigDecimal amount, List<Rate> rates, String grade, LocalDate date,
            BigDecimal floor) {
        return Rate.inForce(rates, grade, date)
                .map((pct) -> amount.multiply(pct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN))
                .map((due) -> due.max(floor))
                .orElse(null);
    }
}
