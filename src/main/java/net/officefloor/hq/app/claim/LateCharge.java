package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * What an overdue claim is charged: the sheet's late percent of its figure for every full 30 days it is overdue
 * (its due-by, its date plus the terms, before the sheet's today), rounded like the claim's due. Never bound by a statement:
 * it follows the sheet's today until the claim is settled. A correction is never charged.
 */
public record LateCharge(int terms, BigDecimal pct, LocalDate today) {

    /** The days in each period an overdue claim is charged for. */
    public static final int PERIOD_DAYS = 30;

    /** The charge on the claim's figure (null: n/a, charged nothing); zero unless the claim is overdue. */
    public BigDecimal on(Claim claim, BigDecimal figure, RoundingMode mode) {
        if (figure == null || claim.status() != ClaimStatus.OPEN || claim.correction()) {
            return BigDecimal.ZERO.setScale(2);
        }
        long late = ChronoUnit.DAYS.between(claim.date().plusDays(terms), today);
        long periods = late > 0 ? late / PERIOD_DAYS : 0;
        return Claim.due(figure, pct.multiply(BigDecimal.valueOf(periods)), mode);
    }
}
