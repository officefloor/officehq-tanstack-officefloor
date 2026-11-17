package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;

/**
 * The late charge on a sheet as of its today: an overdue claim (OPEN or PENDING, its due-by — fixed when it was added — plus
 * its grace before today; see lateFrom) gains, for every full 30 days it is overdue, the late percent in force on that boundary
 * date (its due-by plus its grace plus 30, 60, 90 ... days) of its figure plus the charges of the boundaries before it (compounding), each rounded to two decimals by the claim's rounding (see Rounding), added up; a
 * boundary whose percent is 0.00 adds nothing. A statement binds the claim's due as shown, the charge included: while
 * bound it no longer follows today (see #add); settling freezes it into the claim's due.
 * A claim whose late charge a steward waived bears none (see ApplyWaive).
 * The floor is applied first: a claim's live due is its figure floored (the greater of it and the floor in force on its date)
 * plus the charge worked out on that floored figure.
 */
public record LateCharge(LocalDate today, Function<LocalDate, BigDecimal> pct, Rounding rounding,
        Function<Claim, Integer> grace, java.util.function.Predicate<Claim> waived) {

    /** The days in each step of the charge. */
    public static final int DAYS = 30;

    public static LateCharge of(String sheetId, SheetRepository sheets, SettingRepository settings,
            ClaimRepository claims) {
        return new LateCharge(sheets.today(sheetId), (date) -> settings.latePct(sheetId, date),
                Rounding.of(sheetId, sheets, settings, claims),
                (claim) -> settings.graceOf(sheetId, claim.personId(), claim.date()),
                waivedIn(claims.waived(sheetId, null)));
    }

    /** Whether a claim is waived: its id is among the given ones. */
    public static java.util.function.Predicate<Claim> waivedIn(java.util.Set<Long> ids) {
        return (claim) -> ids.contains(claim.id());
    }

    /** The date a claim is late from: its due-by plus the grace of its person's grade in force on its date (none: its
     *  due-by). The claim is overdue once this is before today; its 30-day boundaries count from it. */
    public LocalDate lateFrom(Claim claim) {
        return claim.dueBy().plusDays(grace.apply(claim));
    }

    /** The charge on the claim with the given figure: zero when it is not overdue a full 30 days, has no figure, is
     *  a correction, is deferred, or is waived. */
    public BigDecimal on(Claim claim, BigDecimal figure) {
        if (figure == null || !ClaimDue.isOpen(claim) || claim.due() != null || ClaimDue.isCorrection(claim)
                || ClaimDue.deferred(claim, today) || waived.test(claim)) {
            return BigDecimal.ZERO.setScale(2);
        }
        LocalDate from = lateFrom(claim);
        long overdue = ChronoUnit.DAYS.between(from, today);
        long steps = overdue <= 0 ? 0 : overdue / DAYS;
        BigDecimal charge = BigDecimal.ZERO.setScale(2);
        for (long step = 1; step <= steps; step++) {
            BigDecimal boundary = pct.apply(from.plusDays(step * DAYS));
            charge = charge.add(figure.add(charge).multiply(boundary).movePointLeft(2).setScale(2, rounding.mode(claim)));
        }
        return charge;
    }

    /** The claim's due: the figure, at least the floor (none for a correction, nor for a settled figure, which stays
     *  frozen), with the charge on that floored figure added on top; null when it has none. A claim bound by a statement (not settled) has the
     *  due it was bound to, its figure, as it is. */
    public BigDecimal add(Claim claim, BigDecimal figure, BigDecimal floor) {
        if (figure == null) {
            return null;
        }
        if (claim.due() == null && ClaimDue.bound(claim)) {
            return figure;
        }
        BigDecimal floored = claim.due() != null || ClaimDue.isCorrection(claim) ? figure : figure.max(floor);
        return floored.add(on(claim, floored));
    }
}
