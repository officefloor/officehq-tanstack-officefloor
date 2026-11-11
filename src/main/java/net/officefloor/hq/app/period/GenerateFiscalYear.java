package net.officefloor.hq.app.period;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Makes the twelve monthly periods of fiscal year FY<yyyy>, from the first day of the sheet's fiscal
 * start month in that year, exactly as the period generator does: a month whose name is already a
 * period, or whose days overlap a period on the sheet, is left out; each made is saved and audited.
 */
public class GenerateFiscalYear {
    public void generate(@Val FiscalYear fiscalYear, @Val Sheet sheet, SettingsRepository settings,
            PeriodRepository repository, HttpServletRequest request, Audit audit, Out<GeneratePeriodsResponse> result) {
        LocalDate from = LocalDate.of(fiscalYear.year(), settings.find(sheet.id()).fiscalStart(), 1);
        result.set(new GeneratePeriodsResponse(
                GeneratePeriods.months(from, from.plusMonths(12).minusDays(1), sheet, repository, request, audit)));
    }
}
