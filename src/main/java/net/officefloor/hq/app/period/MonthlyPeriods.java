package net.officefloor.hq.app.period;

import java.time.YearMonth;
import net.officefloor.hq.app.sheet.Sheet;

/** The period generator for other features (not a step): adds a period per calendar month exactly as it does. */
public final class MonthlyPeriods {

    private MonthlyPeriods() {
    }

    /** Adds a period per calendar month from the first month to the last, as the period generator does. */
    public static GeneratedPeriods generate(Sheet sheet, YearMonth firstMonth, YearMonth lastMonth, PeriodRepository repository) {
        return GeneratePeriods.months(sheet, firstMonth, lastMonth, repository);
    }
}
