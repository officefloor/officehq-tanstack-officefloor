package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.rate.Rate;

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

    /** The amount at the grade's rate in force on the date, at least the floor; null with no such rate. */
    public static BigDecimal live(BigDecimal amount, List<Rate> rates, String grade, LocalDate date,
            BigDecimal floor) {
        return Rate.inForce(rates, grade, date)
                .map((pct) -> amount.multiply(pct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN))
                .map((due) -> due.max(floor))
                .orElse(null);
    }
}
