package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.setting.Dated;
import net.officefloor.hq.app.setting.DatedValue;
import net.officefloor.hq.app.setting.Settings;

/**
 * What makes an overdue claim's late charge: the sheet's late percent (two decimals) by date, the one in force on each 30-day boundary after the claim's due-by, and its today; a claim is due
 * by the due-by fixed when it was added. The charge is never bound by a statement: it follows the sheet's today.
 * It also carries the sheet's policy date, which decides how a claim's live figures round, and each grade's grace
 * as recorded by from-date: the days a claim of a person whose grade in force on its date has a grace in force on
 * that date is given past its due-by before it is overdue.
 */
public record Late(Dated pct, LocalDate today, LocalDate policy, Map<String, List<DatedValue>> grace) {

    /**
     * How the claim's live figures round: half up when it rounds on a date (its own, or for a correction the date of
     * the claim it corrects) before the policy date, half to even on or after it.
     */
    public RoundingMode rounding(Claim claim) {
        return LocalDate.parse(claim.roundsOn()).isBefore(policy) ? RoundingMode.HALF_UP : RoundingMode.HALF_EVEN;
    }

    /**
     * The date the claim's overdue days are counted from: its due-by (fixed when it was added) plus the grace of the
     * grade (its person's in force on its date; null for none) in force on its date, if any.
     */
    public LocalDate graceBy(Claim claim, String grade) {
        LocalDate dueBy = LocalDate.parse(claim.dueBy());
        List<DatedValue> history = grade == null ? null : grace.get(grade);
        String days = history == null ? "" : Settings.on(history, LocalDate.parse(claim.date()));
        return days.isEmpty() ? dueBy : dueBy.plusDays(Integer.parseInt(days));
    }

    /** Whether the claim is overdue: OPEN or PENDING, not deferred, with its {@link #graceBy} before the sheet's today. */
    public boolean overdue(Claim claim, String grade) {
        return !claim.deferred() && (claim.status() == ClaimStatus.OPEN || claim.status() == ClaimStatus.PENDING) && graceBy(claim, grade).isBefore(today);
    }

    /**
     * The claim's late charge on its figure, compounding: for each full 30 days it is overdue, the late percent in force
     * on that boundary (its {@link #graceBy} plus 30, 60, 90 ... days) of the figure plus the charges of the boundaries
     * before it, rounded in hundredths by the claim's {@link #rounding}, the charges added up (a boundary at 0.00 adds
     * nothing); 0.00 when it is not overdue, and always for a correction.
     */
    public BigDecimal charge(Claim claim, String grade, BigDecimal figure) {
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        if (figure == null || claim.correction() || !overdue(claim, grade)) {
            return total;
        }
        LocalDate dueBy = graceBy(claim, grade);
        long periods = ChronoUnit.DAYS.between(dueBy, today) / 30;
        for (long k = 1; k <= periods; k++) {
            BigDecimal boundaryPct = pct.on(dueBy.plusDays(30 * k));
            if (boundaryPct.signum() != 0) {
                total = total.add(figure.add(total).multiply(boundaryPct).movePointLeft(2).setScale(2, rounding(claim)));
            }
        }
        return total;
    }
}
