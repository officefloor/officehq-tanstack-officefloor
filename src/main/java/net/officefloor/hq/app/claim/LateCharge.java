package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;

/**
 * The late charge on a sheet as of its today: an overdue claim (OPEN, its due-by — its date plus the terms — before
 * today) gains the late percent of its figure for every full 30 days it is overdue, rounded half to even to two
 * decimals. The charge is never bound by a statement (it follows today); settling freezes it into the claim's due.
 */
public record LateCharge(LocalDate today, int terms, BigDecimal pct) {

    /** The days in each step of the charge. */
    public static final int DAYS = 30;

    public static LateCharge of(String sheetId, SheetRepository sheets, SettingRepository settings) {
        return new LateCharge(sheets.today(sheetId), settings.terms(sheetId), settings.latePct(sheetId));
    }

    /** The charge on the claim with the given figure: zero when it is not overdue a full 30 days or has no figure. */
    public BigDecimal on(Claim claim, BigDecimal figure) {
        if (figure == null || claim.status() != ClaimStatus.OPEN || claim.due() != null) {
            return BigDecimal.ZERO.setScale(2);
        }
        long overdue = ChronoUnit.DAYS.between(claim.date().plusDays(terms), today);
        long steps = overdue <= 0 ? 0 : overdue / DAYS;
        return figure.multiply(pct).multiply(BigDecimal.valueOf(steps)).movePointLeft(2)
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    /** The figure with its charge added on top; null when it has none. */
    public BigDecimal add(Claim claim, BigDecimal figure) {
        return figure == null ? null : figure.add(on(claim, figure));
    }
}
