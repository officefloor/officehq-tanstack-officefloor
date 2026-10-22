package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;

/** The sheet's late charge as it is today: its terms (days; a claim's due-by is its date plus them), its late
 *  percent and its today (null: none, so nothing is overdue). */
public record LateCharge(int terms, BigDecimal pct, LocalDate today) {

    /** The sheet's late charge as it is now. */
    public static LateCharge of(String sheetId, SettingRepository settings, SheetRepository sheets) {
        return new LateCharge(settings.terms(sheetId), settings.latePct(sheetId), sheets.today(sheetId));
    }

    /** No late charge at all. */
    public static LateCharge none() {
        return new LateCharge(0, BigDecimal.ZERO, null);
    }

    /** Whether a claim is overdue: it is OPEN and its due-by is before today. */
    public boolean overdue(Claim claim) {
        return claim.status() == ClaimStatus.OPEN && today != null
                && claim.date().plusDays(terms).isBefore(today);
    }

    /** The charge on a claim whose figure is `figure`: the late percent of the figure for every full 30 days it
     *  is overdue, rounded half to even to two decimals; 0.00 when it is not overdue, or it is a correction (a negative amount). */
    public BigDecimal on(Claim claim, BigDecimal figure) {
        if (!overdue(claim) || pct.signum() == 0 || claim.amount().signum() < 0) {
            return new BigDecimal("0.00");
        }
        long periods = ChronoUnit.DAYS.between(claim.date().plusDays(terms), today) / 30;
        return figure.multiply(pct).multiply(BigDecimal.valueOf(periods))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN);
    }
}
