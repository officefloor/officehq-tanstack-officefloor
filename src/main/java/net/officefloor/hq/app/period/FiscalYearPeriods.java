package net.officefloor.hq.app.period;

import java.time.YearMonth;
import net.officefloor.hq.app.sheet.Sheet;

/** The fiscal year's twelve monthly periods from its first month, made as {@link BuildFiscalYearPeriods} makes them. */
public final class FiscalYearPeriods {

    private FiscalYearPeriods() {
    }

    public static GeneratedPeriods of(Sheet sheet, PeriodRepository repository, YearMonth first) {
        return BuildGeneratedPeriods.months(sheet, repository, first, first.plusMonths(11));
    }
}
