package net.officefloor.hq.app.period;

import jakarta.validation.Valid;
import java.time.YearMonth;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * The fiscal year's twelve monthly periods, from the sheet's fiscal start month in the year typed, made as
 * {@link BuildGeneratedPeriods} makes them. A year that is not four digits is refused.
 */
public class BuildFiscalYearPeriods {
    public void build(@RequestBody @Valid GenerateFiscalYearRequest request, @Val Sheet sheet, PeriodRepository repository,
            SettingRepository settings, Out<GeneratedPeriods> generated) throws InvalidPeriodException {
        String year = request.year() == null ? "" : request.year().trim();
        if (!year.matches("\\d{4}")) {
            throw new InvalidPeriodException("The fiscal year must be yyyy.");
        }
        YearMonth first = YearMonth.of(Integer.parseInt(year), settings.fiscalStartMonth(sheet.id()));
        generated.set(BuildGeneratedPeriods.months(sheet, repository, first, first.plusMonths(11)));
    }
}
