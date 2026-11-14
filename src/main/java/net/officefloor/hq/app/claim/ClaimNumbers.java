package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;

/** The numbers claims are given on their sheet. */
public final class ClaimNumbers {

    private ClaimNumbers() {
    }

    /**
     * The number opening the claim gives it now: FY&lt;year&gt;-&lt;n&gt;, the tree's next within the fiscal year its
     * due-by falls in (named by the year it starts in), or the next within its period under period numbering.
     */
    public static String next(Claim claim, SheetRepository sheets, SettingRepository settings,
            PeriodRepository periods) {
        LocalDate from = settings.periodNumberingFrom(claim.sheetId());
        if (from == null || sheets.today(claim.sheetId()).isBefore(from)) {
            int year = fiscalYear(claim.dueBy(), settings.fiscalStartMonth(claim.sheetId()));
            return "FY" + year + "-" + sheets.nextFiscalClaimNumber(claim.sheetId(), year);
        }
        Period period = periods.findAll(claim.sheetId()).stream().filter((p) -> p.contains(claim.dueBy())).findFirst()
                .orElse(null);
        String prefix = period == null ? "X" : period.name();
        String key = period == null ? "" : period.name().toLowerCase();
        return prefix + "-" + sheets.nextPeriodClaimNumber(claim.sheetId(), key);
    }

    /** The fiscal year a date falls in, named by the calendar year it starts in. */
    static int fiscalYear(LocalDate date, int startMonth) {
        return date.getMonthValue() >= startMonth ? date.getYear() : date.getYear() - 1;
    }
}
