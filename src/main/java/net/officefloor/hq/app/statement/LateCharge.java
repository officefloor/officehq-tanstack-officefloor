package net.officefloor.hq.app.statement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.setting.DatedAmount;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;

/** The sheet's late charge as it is today: its late percent by date (a claim gains the one in force on each 30-day boundary past its due-by; a claim is overdue past its own due-by, fixed when
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

    /** Whether a claim is overdue: it is OPEN or PENDING, not deferred on today, and its due-by is before today. */
    public boolean overdue(Claim claim) {
        return claim.status().unsettled() && today != null && !claim.deferredOn(today)
                && claim.dueBy().isBefore(today);
    }

    /** The charge on a claim whose figure is `figure`: for each full 30 days it is overdue, the late percent in force
     *  on that boundary date (its due-by plus 30, 60, 90 ... days) of the figure, rounded to two decimals as the claim
     *  rounds (see Rounding), the charges added up (a boundary whose percent is 0.00 adds nothing); 0.00 when it is not
     *  overdue, or it is a correction (a negative amount). */
    public BigDecimal on(Claim claim, BigDecimal figure) {
        BigDecimal total = new BigDecimal("0.00");
        if (!overdue(claim) || claim.amount().signum() < 0) {
            return total;
        }
        long periods = ChronoUnit.DAYS.between(claim.dueBy(), today) / 30;
        for (long n = 1; n <= periods; n++) {
            BigDecimal pct = this.pct.on(claim.dueBy().plusDays(30 * n));
            if (pct.signum() != 0) {
                total = total.add(figure.multiply(pct).divide(BigDecimal.valueOf(100), 2, rounding.of(claim)));
            }
        }
        return total;
    }
}
