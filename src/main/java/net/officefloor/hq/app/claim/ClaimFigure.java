package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** A claim's figure: the due fixed when settled, else the rate figure its person's last statement bound it
 *  to, else its amount at the grade rate in force on
 *  its date, rounded as the sheet's {@link Rounding} has it (null, n/a, when no rate applies). The floor is not
 *  part of the figure: {@link Lateness#due} applies it last, after the late charge. */
public final class ClaimFigure {
    private ClaimFigure() {
    }

    /** amount × pct ÷ 100, rounded by {@code mode} to two decimals; null when there is no rate. */
    public static BigDecimal of(BigDecimal amount, BigDecimal pct, RoundingMode mode) {
        return pct == null ? null : amount.multiply(pct).divide(BigDecimal.valueOf(100), 2, mode);
    }

    /** The due raised to the floor; null stays null. */
    public static BigDecimal floored(BigDecimal due, BigDecimal floor) {
        return due == null || due.compareTo(floor) >= 0 ? due : floor.setScale(2);
    }

    /** The figure the claim has now, given the grade's rate in force on its date. */
    public static BigDecimal of(Claim claim, BigDecimal pct, Rounding rounding) {
        if (claim.getSettledDue() != null) {
            return claim.getSettledDue();
        }
        if (claim.isBound()) {
            return claim.getStatedDue();
        }
        return of(claim.getAmount(), pct, rounding.mode(claim));
    }
}
