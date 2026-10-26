package net.officefloor.hq.app.access;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.person.Person;

/**
 * The late charge as of the sheet's today: an OPEN (or PENDING) claim past its due-by (the terms-th business day after its date, by the terms fixed when it was added and the sheet's holidays now)
 * plus its grade's grace (see {@link #lateFrom}) gains, for every
 * full 30 days it is overdue, the late percent in force on that boundary date (that date plus 30, 60, 90 ... days) of its figure
 * plus the charges of the boundaries before it (compounding),
 * each rounded to two decimals as the claim rounds (see {@link #rounding}), the charges added up. A correction
 * (a negative amount) never gains one, nor does a claim whose late charge a steward waived. Settling freezes it into the
 * settled due; a statement binds the due as shown (late charge and floor included), so a bound claim's late charge no
 * longer follows today until the statement is cancelled.
 * The floor is applied last: a claim's live due is the greater of the floor in force on its date and its figure plus its
 * late charge (a correction has no floor).
 */
public record LateCharge(LocalDate today, DatedSettings settings, LocalDate policyDate, Map<Long, Person> people) {

    /**
     * The date the claim's lateness counts from: its due-by plus the grace in force on its date of its person's grade
     * on that date (none while blank). It is overdue once this is before today; the 30-day boundaries count from it.
     */
    public LocalDate lateFrom(Claim claim) {
        LocalDate dueBy = claim.getDueBy();
        Person person = people.get(claim.getPersonId());
        if (dueBy == null || person == null) {
            return dueBy;
        }
        return dueBy.plusDays(settings.grace(person.gradeOn(claim.getDate()), claim.getDate()));
    }

    /** Whether the claim is overdue as of today: OPEN or PENDING, and its due-by plus its grace before today. */
    public boolean overdue(Claim claim) {
        LocalDate from = lateFrom(claim);
        return claim.getStatus().isOpen() && from != null && from.isBefore(today);
    }

    /**
     * How the claim's live figures round: by the rounding mode in force on its date when there is one; else half up
     * when it is dated before the policy date, half to even on or after it. A correction rounds by the date of the
     * claim it corrects.
     */
    public RoundingMode rounding(Claim claim) {
        RoundingMode mode = settings.rounding(claim.getRoundOn());
        if (mode != null) {
            return mode;
        }
        return claim.getRoundOn().isBefore(policyDate) ? RoundingMode.HALF_UP : RoundingMode.HALF_EVEN;
    }

    /** The claim's late charge on its figure (zero when it is not overdue, is deferred past today, or has no figure). */
    public BigDecimal on(Claim claim, BigDecimal figure) {
        BigDecimal none = BigDecimal.ZERO.setScale(2);
        if (!claim.getStatus().isOpen() || claim.isDeferredOn(today) || bound(claim)) {
            return none;
        }
        return onAsOpen(claim, figure);
    }

    /** The late charge the claim would have on its figure were it OPEN. */
    public BigDecimal onAsOpen(Claim claim, BigDecimal figure) {
        BigDecimal none = BigDecimal.ZERO.setScale(2);
        if (figure == null || claim.isCorrection() || claim.isWaived()) {
            return none;
        }
        LocalDate from = lateFrom(claim);
        long overdue = ChronoUnit.DAYS.between(from, today);
        long periods = overdue > 0 ? overdue / 30 : 0;
        if (periods == 0) {
            return none;
        }
        BigDecimal charge = none;
        for (long n = 1; n <= periods; n++) {
            BigDecimal pct = settings.latePct(from.plusDays(30 * n));
            if (pct.signum() != 0) {
                charge = charge.add(figure.add(charge).multiply(pct).movePointLeft(2).setScale(2, rounding(claim)));
            }
        }
        return charge;
    }

    /**
     * The claim's due: its settled due, or the due a statement bound it to, as it is; else the greater of the floor in force on its date and the figure with
     * its late charge added on top (a correction has no floor); null without a figure.
     */
    public BigDecimal due(Claim claim, BigDecimal figure) {
        if (figure == null) {
            return null;
        }
        if (claim.getSettledDue() != null || bound(claim)) {
            return figure.setScale(2);
        }
        return floored(claim, figure.setScale(2).add(on(claim, figure)));
    }

    /** The due the claim would have were it OPEN (floored as {@link #due}; a bound due as it is); null without a figure. */
    public BigDecimal dueAsOpen(Claim claim, BigDecimal figure) {
        if (figure != null && bound(claim)) {
            return figure.setScale(2);
        }
        return figure == null ? null : floored(claim, figure.setScale(2).add(onAsOpen(claim, figure)));
    }

    /** Whether a statement bound the claim's due (late charge included) and it is not settled: its figure is that due. */
    private static boolean bound(Claim claim) {
        return claim.isBound() && claim.getSettledDue() == null;
    }

    /** At least the floor in force on the claim's date, unless it is a correction. */
    private BigDecimal floored(Claim claim, BigDecimal due) {
        return claim.isCorrection() ? due : due.max(settings.floor(claim.getDate()));
    }
}
