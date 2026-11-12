package net.officefloor.hq.app.period;

import java.time.YearMonth;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Makes a period for each month in the range that is free (its name unused and its days not
 *  overlapping a period on the sheet), recording each as PERIOD_ADDED, as an added period is. */
public class GeneratePeriods {
    public void generate(@PathVariable("sheetId") String sheetId, @Val GenerateInput input, PeriodStore store,
            Audit audit, Out<GenerateResult> result) {
        String sheet = sheetId.trim();
        int made = 0;
        for (YearMonth month = input.from(); !month.isAfter(input.to()); month = month.plusMonths(1)) {
            PeriodInput period = new PeriodInput(month.toString(), month.atDay(1), month.atEndOfMonth(), input.user());
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
