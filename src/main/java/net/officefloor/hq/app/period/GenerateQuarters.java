package net.officefloor.hq.app.period;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** One period per calendar quarter of the year, named yyyy-Q1 to yyyy-Q4 and running from its first to its last
 *  day, each added as the period form would add it; a quarter whose name is taken, or whose days overlap a period
 *  on the sheet, is left out. */
public class GenerateQuarters {
    public void generate(@PathVariable("sheetId") String sheetId, @Val ValidQuarters request, Periods periods,
            Audit audit, ObjectResponse<GenerateResponse> response) {
        int generated = 0;
        for (int quarter = 1; quarter <= 4; quarter++) {
            LocalDate from = LocalDate.of(request.year(), quarter * 3 - 2, 1);
            ValidPeriod period = new ValidPeriod(request.year() + "-Q" + quarter, from,
                    from.plusMonths(3).minusDays(1), request.user());
            if (periods.addIfFree(sheetId, period)) {
                generated++;
                audit.recordOnCommit("PERIOD_ADDED sheet=" + sheetId + " by=" + period.user()
                        + " period=" + period.name() + " from=" + period.from() + " to=" + period.to());
            }
        }
        response.send(new GenerateResponse(generated, periods.list(sheetId)));
    }
}
