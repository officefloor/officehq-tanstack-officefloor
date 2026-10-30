package net.officefloor.hq.app.period;

import java.time.YearMonth;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** One period per calendar month from the from-date's month to the to-date's month, named yyyy-mm and
 *  running from its first to its last day; a month whose name is taken, or whose days overlap a period
 *  on the sheet, is left out. */
public class GeneratePeriods {
    public void generate(@PathVariable("sheetId") String sheetId, @Val ValidGenerate request, Periods periods,
            Audit audit, ObjectResponse<GenerateResponse> response) {
        int generated = 0;
        YearMonth last = YearMonth.from(request.to());
        for (YearMonth month = YearMonth.from(request.from()); !month.isAfter(last); month = month.plusMonths(1)) {
            ValidPeriod period = new ValidPeriod(month.toString(), month.atDay(1), month.atEndOfMonth(),
                    request.user());
            if (periods.addIfFree(sheetId, period)) {
                generated++;
                audit.recordOnCommit("PERIOD_ADDED sheet=" + sheetId + " by=" + period.user()
                        + " period=" + period.name() + " from=" + period.from() + " to=" + period.to());
            }
        }
        response.send(new GenerateResponse(generated, periods.list(sheetId)));
    }
}
