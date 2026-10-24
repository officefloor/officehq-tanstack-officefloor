package net.officefloor.hq.app.period;

import java.time.YearMonth;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the fiscal year's twelve monthly periods, from its first month (the sheet's fiscal year start month of the
 *  year) on, exactly as the period generator does. */
public class GenerateFiscalYear {
    public void generate(@Val GenerateFiscalYearRequest request, @Val Sheet sheet, PeriodRepository repository,
            Out<GeneratedPeriods> generatedOut) {
        YearMonth first = YearMonth.of(Integer.parseInt(request.year()), sheet.getFiscalStartMonth());
        generatedOut.set(GeneratePeriods.months(sheet, first, first.plusMonths(11), repository));
    }
}
