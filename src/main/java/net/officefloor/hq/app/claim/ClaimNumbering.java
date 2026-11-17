package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;

/**
 * The number a claim receives as it is settled or approved (see SaveClaimStatus). While the sheet's period-numbering-from setting is a date and the
 * sheet's today is on or after it, the claim is numbered within the period containing its due-by, {@code <period>-<n>}
 * counting from 1 in each period, or {@code X-<n>} in its own sequence when no period contains it. Otherwise it takes
 * the next number of the sheet's tree within the fiscal year of its due-by (by the sheet's fiscal start month),
 * {@code FY<yyyy>-<n>}: one more than the last number given in that fiscal year on any sheet of the tree, counting from
 * 1 in each fiscal year, so the sheets of a tree share one sequence per fiscal year.
 */
public final class ClaimNumbering {

    private ClaimNumbering() {
    }

    public static String next(Claim claim, ClaimRepository claims, PeriodRepository periods, SettingRepository settings,
            SheetRepository sheets) {
        String sheet = claim.sheetId();
        if (!perPeriod(sheet, settings, sheets)) {
            String fy = fiscalYearOf(claim.dueBy(), settings.fiscalStartMonth(sheet));
            return fy + "-" + claims.nextFiscalYearNumber(sheet, settings.tree(sheet), fy);
        }
        Period period = periods.containing(sheet, claim.dueBy());
        String key = period == null ? "X" : period.name();
        return key + "-" + claims.nextPeriodNumber(sheet, key);
    }

    /** The fiscal year a date falls in, FY<yyyy> by the calendar year its first day (of the start month) falls in. */
    public static String fiscalYearOf(LocalDate date, int startMonth) {
        return "FY" + (date.getMonthValue() >= startMonth ? date.getYear() : date.getYear() - 1);
    }

    /** Whether claims are numbered per period now: the period-numbering-from setting is a date on or before today. */
    public static boolean perPeriod(String sheet, SettingRepository settings, SheetRepository sheets) {
        LocalDate from = settings.periodNumberingFrom(sheet);
        return from != null && !sheets.today(sheet).isBefore(from);
    }
}
