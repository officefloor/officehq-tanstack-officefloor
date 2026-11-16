package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;

/**
 * The late charge on a sheet as of its today: an overdue claim (OPEN or PENDING, its due-by — fixed when it was added — before
 * today) gains, for every full 30 days it is overdue, the late percent in force on that boundary date (its due-by plus 30,
 * 60, 90 ... days) of its figure, each rounded to two decimals by the claim's rounding (see Rounding), added up; a
 * boundary whose percent is 0.00 adds nothing. The charge is never bound by a statement (it follows today); settling freezes it into the claim's due.
 */
public record LateCharge(LocalDate today, Function<LocalDate, BigDecimal> pct, Rounding rounding) {

    /** The days in each step of the charge. */
    public static final int DAYS = 30;

    public static LateCharge of(String sheetId, SheetRepository sheets, SettingRepository settings,
            ClaimRepository claims) {
        return new LateCharge(sheets.today(sheetId), (date) -> settings.latePct(sheetId, date),
                Rounding.of(sheetId, sheets, settings, claims));
    }

    /** The charge on the claim with the given figure: zero when it is not overdue a full 30 days, has no figure, is
     *  a correction, or is deferred. */
    public BigDecimal on(Claim claim, BigDecimal figure) {
        if (figure == null || !ClaimDue.isOpen(claim) || claim.due() != null || ClaimDue.isCorrection(claim)
                || ClaimDue.deferred(claim, today)) {
            return BigDecimal.ZERO.setScale(2);
        }
        long overdue = ChronoUnit.DAYS.between(claim.dueBy(), today);
        long steps = overdue <= 0 ? 0 : overdue / DAYS;
        BigDecimal charge = BigDecimal.ZERO.setScale(2);
        for (long step = 1; step <= steps; step++) {
            BigDecimal boundary = pct.apply(claim.dueBy().plusDays(step * DAYS));
            charge = charge.add(figure.multiply(boundary).movePointLeft(2).setScale(2, rounding.mode(claim)));
        }
        return charge;
    }

    /** The figure with its charge added on top; null when it has none. */
    public BigDecimal add(Claim claim, BigDecimal figure) {
        return figure == null ? null : figure.add(on(claim, figure));
    }
}
