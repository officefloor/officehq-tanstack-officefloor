package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.YearMonth;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Adds the year's four calendar quarters as periods named yyyy-Q1 to yyyy-Q4, first to last day, each recorded as
 * PERIOD_ADDED; a quarter whose name is in use on the sheet or whose days overlap a period is left out.
 */
public class GenerateQuarters {
    public void generate(@Val SheetResponse sheet, @Val QuarterYear year, @Val ChangedBy by,
            PeriodRepository repository, Audit audit, Out<GeneratePeriodsResponse> result) {
        int made = 0;
        for (int quarter = 1; quarter <= 4; quarter++) {
            String name = year.year() + "-Q" + quarter;
            LocalDate first = YearMonth.of(year.year(), quarter * 3 - 2).atDay(1);
            LocalDate last = YearMonth.of(year.year(), quarter * 3).atEndOfMonth();
            if (repository.nameTaken(sheet.id(), name) || repository.overlaps(sheet.id(), first, last)) {
                continue;
            }
            new RecordPeriodAdded().record(repository.insert(sheet.id(), name, first, last), by, audit);
            made++;
        }
        result.set(new GeneratePeriodsResponse(made));
    }
}
