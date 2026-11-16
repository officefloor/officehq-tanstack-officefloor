package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;

/**
 * The late charge on a sheet as of its today: an overdue claim (OPEN or PENDING, its due-by — fixed when it was added — before
 * today) gains the late percent in force on its date of its figure for every full 30 days it is overdue, rounded to two
 * decimals by the claim's rounding (see Rounding). The charge is never bound by a statement (it follows today); settling freezes it into the claim's due.
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
        return figure.multiply(pct.apply(claim.date())).multiply(BigDecimal.valueOf(steps)).movePointLeft(2)
                .setScale(2, rounding.mode(claim));
    }

    /** The figure with its charge added on top; null when it has none. */
    public BigDecimal add(Claim claim, BigDecimal figure) {
        return figure == null ? null : figure.add(on(claim, figure));
    }
}
