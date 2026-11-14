package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;

/** The number a claim gets when opened: while the sheet's period numbering is in force (its
 *  period-numbering-from is a date on or before the sheet's today) the next in the sequence of the
 *  period its date falls in ({@code <period>-<n>}; {@code X-<n>} in no period), else the sheet's next. */
public final class ClaimNumbers {
    private ClaimNumbers() {
    }

    public static String next(Claim claim, ClaimRepository claims, PeriodRepository periods,
            SettingRepository settings, SheetRepository sheets) {
        String sheetId = claim.getSheetId();
        LocalDate from = settings.periodNumberingFrom(sheetId);
        if (from == null || sheets.today(sheetId).isBefore(from)) {
            return claims.nextNumber(sheetId);
        }
        String period = periods.list(sheetId).stream().filter((p) -> p.contains(claim.getDate()))
                .map(Period::name).findFirst().orElse(null);
        return claims.nextPeriodNumber(sheetId, period);
    }
}
