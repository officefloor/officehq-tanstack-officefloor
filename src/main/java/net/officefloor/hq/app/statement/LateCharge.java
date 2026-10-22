package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.setting.DatedAmount;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;

/** The sheet's late charge as it is today: its late percent by date (a claim gains the one in force on its date; a claim is overdue past its own due-by, fixed when
 *  it was created) and its today (null: none, so nothing is overdue). */
public record LateCharge(DatedAmount pct, LocalDate today, Rounding rounding) {

    /** The sheet's late charge as it is now. */
    public static LateCharge of(String sheetId, SettingRepository settings, SheetRepository sheets,
            ClaimRepository claims) {
        return new LateCharge(settings.latePct(sheetId), sheets.today(sheetId),
                Rounding.of(sheetId, settings, sheets, claims));
    }

    /** No late charge at all. */
    public static LateCharge none() {
        return new LateCharge(DatedAmount.always(BigDecimal.ZERO), null, Rounding.none());
    }

    /** Whether a claim is overdue: it is OPEN or PENDING and its due-by is before today. */
    public boolean overdue(Claim claim) {
        return claim.status().unsettled() && today != null
                && claim.dueBy().isBefore(today);
    }

    /** The charge on a claim whose figure is `figure`: the late percent in force on its date, of the figure, for every full 30 days it
     *  is overdue, rounded to two decimals as the claim rounds (see Rounding); 0.00 when it is not overdue, or it is a correction (a negative amount). */
    public BigDecimal on(Claim claim, BigDecimal figure) {
        BigDecimal pct = this.pct.on(claim.date());
        if (!overdue(claim) || pct.signum() == 0 || claim.amount().signum() < 0) {
            return new BigDecimal("0.00");
        }
        long periods = ChronoUnit.DAYS.between(claim.dueBy(), today) / 30;
        return figure.multiply(pct).multiply(BigDecimal.valueOf(periods))
                .divide(BigDecimal.valueOf(100), 2, rounding.of(claim));
    }
}
