package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;

/** The number a claim gets when opened: while the sheet's period numbering is in force (its
 *  period-numbering-from is a date on or before the sheet's today) the next in the sequence of the
 *  period its due-by falls in ({@code <period>-<n>}; {@code X-<n>} in no period), else the next in the
 *  sequence the sheets of its tree share for the fiscal year its due-by falls in ({@code FY<yyyy>-<n>},
 *  restarting from 1 each fiscal year, by the sheet's fiscal year start month; see
 *  {@link SheetRepository#tree}). */
public final class ClaimNumbers {
    private ClaimNumbers() {
    }

    public static String next(Claim claim, ClaimRepository claims, PeriodRepository periods,
            SettingRepository settings, SheetRepository sheets) {
        String sheetId = claim.getSheetId();
        LocalDate from = settings.periodNumberingFrom(sheetId);
        if (from == null || sheets.today(sheetId).isBefore(from)) {
            return claims.nextFiscalYearNumber(sheetId, sheets.tree(sheetId),
                    fiscalYear(LocalDate.parse(claim.getDueBy() == null ? claim.getDate() : claim.getDueBy()), settings.fiscalStartMonth(sheetId)));
        }
        String period = periods.list(sheetId).stream().filter((p) -> p.contains(claim.getDueBy()))
                .map(Period::name).findFirst().orElse(null);
        return claims.nextPeriodNumber(sheetId, period);
    }

    /** The fiscal year a date falls in, FY<yyyy> by the calendar year of the fiscal year's first day. */
    static String fiscalYear(LocalDate date, int startMonth) {
        return "FY" + (date.getMonthValue() >= startMonth ? date.getYear() : date.getYear() - 1);
    }
}
