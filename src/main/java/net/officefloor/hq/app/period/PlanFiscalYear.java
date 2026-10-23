package net.officefloor.hq.app.period;

import java.time.YearMonth;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Turn the fiscal year into the twelve months it runs: from the sheet's fiscal year start month of its year. */
public class PlanFiscalYear {
    public void plan(@PathVariable("sheetId") String sheetId, @Val FiscalYearGenerateInput input,
            SettingRepository settings, Out<PeriodGenerateInput> body) {
        YearMonth first = YearMonth.of(input.year(), settings.fiscalStartMonth(sheetId.trim()));
        body.set(new PeriodGenerateInput(input.user(), first, first.plusMonths(11)));
    }
}
