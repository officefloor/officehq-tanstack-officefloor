package net.officefloor.hq.app.period;

import java.time.YearMonth;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** Makes fiscal year FY<yyyy>'s twelve monthly periods, from the sheet's fiscal year start month of that
 *  year, exactly as the period generator does (a month whose name is taken or whose days overlap a period
 *  is left out). A year that is not four digits is refused and nothing is written. */
public class GenerateFiscalYear {
    public void generate(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by,
            @RequestBody GenerateFiscalYearRequest request, SheetRepository sheets, SettingRepository settings,
            PeriodRepository periods, Audit audit, Out<GeneratePeriodsResult> result) throws InvalidPeriodException {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        String year = request == null || request.year() == null ? "" : request.year().trim();
        if (!year.matches("\\d{4}")) {
            throw new InvalidPeriodException("A fiscal year is four digits (yyyy)");
        }
        YearMonth start = YearMonth.of(Integer.parseInt(year), settings.fiscalStartMonth(id));
        sheets.open(id, user);
        result.set(new GeneratePeriodsResult(
                GeneratePeriods.months(id, user, start, start.plusMonths(11), periods, audit)));
    }
}
