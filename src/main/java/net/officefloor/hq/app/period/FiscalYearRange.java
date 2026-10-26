package net.officefloor.hq.app.period;

import net.officefloor.hq.app.access.SettingRepository;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** The dates of the fiscal year's twelve months, by the sheet's fiscal start month: from its first month's first day to its last month's last. */
public class FiscalYearRange {
    public void range(@Val SheetResponse sheet, @Val FiscalYear fiscalYear, SettingRepository settings,
            Out<GenerateRange> range) {
        int startMonth = settings.fiscalStartMonth(sheet.id());
        range.set(new GenerateRange(fiscalYear.first(startMonth).atDay(1), fiscalYear.last(startMonth).atEndOfMonth()));
    }
}
