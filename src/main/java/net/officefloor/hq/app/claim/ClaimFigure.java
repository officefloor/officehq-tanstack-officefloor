package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** A claim's figure: the due fixed when settled, else the figure its person's last statement bound it
 *  to (not while it is PENDING, which a statement does not bind), else its live due: its amount at the grade rate in force on its date, rounded as the sheet's
 *  {@link Rounding} has it, and
 *  at least the sheet's floor, except a correction's, which has no floor (null, n/a, when no rate applies). */
public final class ClaimFigure {
    private ClaimFigure() {
    }

    /** amount × pct ÷ 100, rounded by {@code mode} to two decimals; null when there is no rate. */
    public static BigDecimal of(BigDecimal amount, BigDecimal pct, RoundingMode mode) {
        return pct == null ? null : amount.multiply(pct).divide(BigDecimal.valueOf(100), 2, mode);
    }

    /** The live due: amount at pct, raised to the floor; null when there is no rate. */
    public static BigDecimal of(BigDecimal amount, BigDecimal pct, BigDecimal floor, RoundingMode mode) {
        BigDecimal due = of(amount, pct, mode);
        return due == null || due.compareTo(floor) >= 0 ? due : floor.setScale(2);
    }

    /** The figure the claim has now, given the grade's rate in force on its date and the sheet's floor. */
    public static BigDecimal of(Claim claim, BigDecimal pct, BigDecimal floor, Rounding rounding) {
        if (claim.getSettledDue() != null) {
            return claim.getSettledDue();
        }
        if (claim.isBound()) {
            return claim.getStatedDue();
        }
        RoundingMode mode = rounding.mode(claim);
        return claim.isCorrection() ? of(claim.getAmount(), pct, mode) : of(claim.getAmount(), pct, floor, mode);
    }
}
