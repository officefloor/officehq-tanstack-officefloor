package net.officefloor.hq.app.period;

import java.time.YearMonth;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Makes a period <yyyy>-Q<n> for each calendar quarter of the year that is free (its name unused and its
 *  days not overlapping a period on the sheet), recording each as PERIOD_ADDED, as an added period is. */
public class GenerateQuarters {
    public void generate(@PathVariable("sheetId") String sheetId, @Val QuartersInput input, PeriodStore store,
            Audit audit, Out<GenerateResult> result) {
        String sheet = sheetId.trim();
        int made = 0;
        for (int quarter = 1; quarter <= 4; quarter++) {
            YearMonth first = YearMonth.of(input.year(), quarter * 3 - 2);
            PeriodInput period = new PeriodInput(input.year() + "-Q" + quarter, first.atDay(1),
                    first.plusMonths(2).atEndOfMonth(), input.user(), null);
            if (store.nameTaken(sheet, period.name()) || store.overlaps(sheet, period.from(), period.to())) {
                continue;
            }
            store.add(sheet, period);
            AuditPeriodAdded.record(sheet, period, audit);
            made++;
        }
        result.set(new GenerateResult(made));
    }
}
