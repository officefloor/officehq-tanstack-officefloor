package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;

/**
 * What an overdue claim is charged: for every full 30 days it is overdue (its due-by, fixed when it was created, plus
 * the grace days of its person's grade in force on its date, before the sheet's today), the late percent in force on
 * that boundary date (the due-by plus the grace plus 30, 60, 90 ... days) of its figure plus the charges of the
 * boundaries before it (compounding), rounded like the claim's due, the charges added up; a boundary whose percent is 0.00 adds nothing. It follows the
 * sheet's today until the claim is settled, or while a statement binds the claim's due (charge included). A correction, a deferred claim and a claim whose
 * charge was waived is never charged.
 */
public record LateCharge(Function<LocalDate, BigDecimal> pctOn, Function<Claim, Integer> graceOf, LocalDate today) {

    /** A charge with no grace days for any claim. */
    public LateCharge(Function<LocalDate, BigDecimal> pctOn, LocalDate today) {
        this(pctOn, (claim) -> 0, today);
    }

    /** The days in each period an overdue claim is charged for. */
    public static final int PERIOD_DAYS = 30;

    /** The charge on the claim's figure (null: n/a, charged nothing); zero unless the claim is overdue. */
    public BigDecimal on(Claim claim, BigDecimal figure, RoundingMode mode) {
        BigDecimal charge = BigDecimal.ZERO.setScale(2);
        if (figure == null || !claim.status().open() || claim.deferred(today) || claim.correction() || claim.waived()) {
            return charge;
        }
        LocalDate lateFrom = claim.dueBy().plusDays(graceOf.apply(claim));
        long late = ChronoUnit.DAYS.between(lateFrom, today);
        long periods = late > 0 ? late / PERIOD_DAYS : 0;
        for (long period = 1; period <= periods; period++) {
            BigDecimal pct = pctOn.apply(lateFrom.plusDays(period * PERIOD_DAYS));
            if (pct.signum() != 0) {
                charge = charge.add(Claim.due(figure.add(charge), pct, mode));
            }
        }
        return charge;
    }
}
