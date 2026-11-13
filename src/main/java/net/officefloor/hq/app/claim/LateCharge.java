package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;

/**
 * What an overdue claim is charged: for every full 30 days it is overdue (its due-by, fixed when it was created, before
 * the sheet's today), the late percent in force on that boundary date (the due-by plus 30, 60, 90 ... days) of its figure,
 * rounded like the claim's due, the charges added up; a boundary whose percent is 0.00 adds nothing. Never bound by a
 * statement: it follows the sheet's today until the claim is settled. A correction, and a deferred claim, is never charged.
 */
public record LateCharge(Function<LocalDate, BigDecimal> pctOn, LocalDate today) {

    /** The days in each period an overdue claim is charged for. */
    public static final int PERIOD_DAYS = 30;

    /** The charge on the claim's figure (null: n/a, charged nothing); zero unless the claim is overdue. */
    public BigDecimal on(Claim claim, BigDecimal figure, RoundingMode mode) {
        BigDecimal charge = BigDecimal.ZERO.setScale(2);
        if (figure == null || !claim.status().open() || claim.deferred(today) || claim.correction()) {
            return charge;
        }
        long late = ChronoUnit.DAYS.between(claim.dueBy(), today);
        long periods = late > 0 ? late / PERIOD_DAYS : 0;
        for (long period = 1; period <= periods; period++) {
            BigDecimal pct = pctOn.apply(claim.dueBy().plusDays(period * PERIOD_DAYS));
            if (pct.signum() != 0) {
                charge = charge.add(Claim.due(figure, pct, mode));
            }
        }
        return charge;
    }
}
