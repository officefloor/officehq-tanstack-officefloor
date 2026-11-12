package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** A claim's figure: the due fixed when settled, else the figure its person's last statement bound it
 *  to, else its amount at the grade rate in force on its date, rounded half to even (null, n/a, when
 *  none applies). */
public final class ClaimFigure {
    private ClaimFigure() {
    }

    /** amount × pct ÷ 100, rounded half to even to two decimals; null when there is no rate. */
    public static BigDecimal of(BigDecimal amount, BigDecimal pct) {
        return pct == null ? null : amount.multiply(pct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN);
    }

    /** The figure the claim has now, given the grade's rate in force on its date. */
    public static BigDecimal of(Claim claim, BigDecimal pct) {
        if (claim.getSettledDue() != null) {
            return claim.getSettledDue();
        }
        if (claim.isStated() && claim.getStatedDue() != null) {
            return claim.getStatedDue();
        }
        return of(claim.getAmount(), pct);
    }
}
